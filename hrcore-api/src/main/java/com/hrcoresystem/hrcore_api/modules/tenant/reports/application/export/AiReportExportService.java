package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.*;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.AiConversationTurnPersistence;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AiReportExportService {
    private static final Logger log = LoggerFactory.getLogger(AiReportExportService.class);
    private final AiConversationTurnPersistence persistence;
    private final AnalyticQueryService queries;
    private final SecurityContextFacade security;
    private final TenantTimezoneService timezone;
    private final AiExportProperties limits;
    private final Semaphore slots;

    public AiReportExportService(AiConversationTurnPersistence persistence, AnalyticQueryService queries,
                                 SecurityContextFacade security, TenantTimezoneService timezone,
                                 AiExportProperties limits) {
        this.persistence = persistence; this.queries = queries; this.security = security;
        this.timezone = timezone; this.limits = limits; this.slots = new Semaphore(limits.getMaxConcurrent());
    }

    public Download generate(UUID conversationId, UUID reportRunId, ExportFormat format) {
        if (!security.isTenantAuthenticated() || !security.hasAuthority("hr.reports.export"))
            throw new AccessDeniedException("No tienes permiso para exportar reportes");
        var source = persistence.loadRun(conversationId, security.requireCurrentUserId(), reportRunId);
        var plan = source.plan();
        if (!"1".equals(source.planVersion()) || !SemanticCatalogV1.VERSION.equals(source.catalogVersion())
                || !source.planVersion().equals(plan.path("version").asText())
                || !source.catalogVersion().equals(plan.path("catalogVersion").asText()))
            throw new BusinessConflictException("El reporte no es compatible con el catálogo actual.");
        if (!slots.tryAcquire()) throw new AiExportBusyException();
        Path path = null;
        try {
            boolean pdf = format == ExportFormat.PDF;
            int maxRows = pdf ? limits.getPdfMaxRows() : limits.getTabularMaxRows();
            long maxBytes = pdf ? limits.getPdfMaxBytes() : limits.getTabularMaxBytes();
            var budget = new ExportBudget(maxBytes, limits.getTimeoutSeconds());
            var generated = ZonedDateTime.now(timezone.requiredZone());
            path = Files.createTempFile("hrcore-ai-export-", ".tmp");
            try (OutputStream output = budget.output(Files.newOutputStream(path));
                 ReportExportDocument document = switch (format) {
                     case CSV -> new CsvExportDocument(output);
                     case XLSX -> new XlsxExportDocument(output);
                     case PDF -> new PdfExportDocument(output, generated, plan);
                 }) {
                queries.export(plan.toString(), maxRows, limits.getTimeoutSeconds(), new AnalyticExportSink() {
                    public long remainingMillis() { return budget.remainingMillis(); }
                    public void columns(List<QueryResultV1.Column> columns) {
                        budget.check();
                        if (pdf && columns.size() > limits.getPdfMaxColumns()) throw new AiExportLimitException();
                        document.columns(columns);
                    }
                    public void row(Map<String, Object> row) {
                        for (Object value : row.values()) {
                            String text = value == null ? "" : value.toString();
                            if (text.length() > 32767) throw new AiExportLimitException();
                            budget.input(text.getBytes(StandardCharsets.UTF_8).length + 16L);
                        }
                        document.row(row);
                    }
                });
                document.finish(); budget.check();
            }
            String filename = "hrcore-reporte-" + generated.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    + "." + format.extension;
            return new Download(path, filename, format.mimeType, Files.size(path), slots);
        } catch (IOException failure) {
            cleanup(path); slots.release(); throw new UncheckedIOException(failure);
        } catch (RuntimeException | Error failure) {
            cleanup(path); slots.release(); throw failure;
        }
    }

    private static void cleanup(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); }
        catch (IOException failure) { log.warn("AI export temporary file cleanup failed"); }
    }

    public static final class Download implements AutoCloseable {
        private final Path path;
        private final String filename, mimeType;
        private final long size;
        private final Semaphore slots;
        private final AtomicBoolean closed = new AtomicBoolean();
        Download(Path path, String filename, String mimeType, long size, Semaphore slots) {
            this.path = path; this.filename = filename; this.mimeType = mimeType; this.size = size; this.slots = slots;
        }
        public Path path() { return path; }
        public String filename() { return filename; }
        public String mimeType() { return mimeType; }
        public long size() { return size; }
        public void close() { if (closed.compareAndSet(false, true)) { cleanup(path); slots.release(); } }
    }
}
