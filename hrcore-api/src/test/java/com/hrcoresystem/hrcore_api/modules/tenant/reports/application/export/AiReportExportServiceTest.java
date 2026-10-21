package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.ai.*;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.AiConversationTurnPersistence;
import java.nio.file.*;
import java.time.ZoneId;
import java.util.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiReportExportServiceTest {
    private final AiConversationTurnPersistence persistence = mock(AiConversationTurnPersistence.class);
    private final AnalyticQueryService queries = mock(AnalyticQueryService.class);
    private final SecurityContextFacade security = mock(SecurityContextFacade.class);
    private final TenantTimezoneService timezone = mock(TenantTimezoneService.class);
    private final AiExportProperties limits = new AiExportProperties();
    private final UUID conversation = UUID.randomUUID(), run = UUID.randomUUID(), owner = UUID.randomUUID();
    private AiReportExportService service;
    private AiConversationTurnPersistence.RunSnapshot source;
    private final List<QueryResultV1.Column> columns = List.of(new QueryResultV1.Column("employee.full_name", SemanticCatalogV1.ValueType.TEXT));

    @BeforeEach void setUp() throws Exception {
        when(security.isTenantAuthenticated()).thenReturn(true);
        when(security.hasAuthority("hr.reports.export")).thenReturn(true);
        when(security.requireCurrentUserId()).thenReturn(owner);
        when(timezone.requiredZone()).thenReturn(ZoneId.of("America/La_Paz"));
        var plan = new ObjectMapper().readTree("{\"version\":\"1\",\"catalogVersion\":\"1\",\"dataset\":\"employees.roster\",\"filters\":[],\"having\":[],\"page\":{\"number\":5,\"size\":1}}");
        source = new AiConversationTurnPersistence.RunSnapshot(run, UUID.randomUUID(), plan, "1", "1");
        when(persistence.loadRun(conversation, owner, run)).thenReturn(source);
        doAnswer(call -> {
            AnalyticExportSink sink = call.getArgument(3); sink.columns(columns);
            sink.row(Map.of("employee.full_name", "Áda Synthetic")); return null;
        }).when(queries).export(anyString(), anyInt(), anyInt(), any());
        service = new AiReportExportService(persistence, queries, security, timezone, limits);
    }

    @Test void createsBoundedFilesAndDeletesThemAfterDownloadWithoutPersistenceWrites() throws Exception {
        for (var format : ExportFormat.values()) {
            Path path;
            try (var result = service.generate(conversation, run, format)) {
                path = result.path(); assertTrue(Files.exists(path));
                assertTrue(result.size() > 0); assertEquals(result.size(), Files.size(path));
                assertTrue(result.filename().matches("hrcore-reporte-[0-9]{8}-[0-9]{6}\\." + format.extension));
                assertFalse(result.filename().contains("Áda"));
            }
            assertFalse(Files.exists(path));
        }
        verify(queries, times(2)).export(eq(source.plan().toString()), eq(10000), eq(30), any());
        verify(queries).export(eq(source.plan().toString()), eq(500), eq(30), any());
        verify(persistence, times(3)).loadRun(conversation, owner, run); verifyNoMoreInteractions(persistence);
    }

    @Test void deniesExportBeforeQueryIfPermissionOwnerRunOrVersionIsInvalid() {
        when(security.hasAuthority("hr.reports.export")).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.generate(conversation, run, ExportFormat.CSV));
        verifyNoInteractions(persistence, queries);
        when(security.hasAuthority("hr.reports.export")).thenReturn(true);
        when(persistence.loadRun(conversation, owner, run)).thenThrow(new ResourceNotFoundException("Reporte no encontrado"));
        assertThrows(ResourceNotFoundException.class, () -> service.generate(conversation, run, ExportFormat.CSV));
        doReturn(new AiConversationTurnPersistence.RunSnapshot(run, source.requestMessageId(), source.plan(), "99", "1"))
                .when(persistence).loadRun(conversation, owner, run);
        assertThrows(BusinessConflictException.class, () -> service.generate(conversation, run, ExportFormat.CSV));
        verifyNoInteractions(queries);
    }

    @Test void cleansTemporaryFileOnQueryFailureRawLimitColumnLimitAndFinalFileLimit() throws Exception {
        Set<Path> before = temporaryExports();
        doThrow(new QueryPlanException(QueryPlanException.Code.QUERY_TIMEOUT)).when(queries).export(anyString(), anyInt(), anyInt(), any());
        assertThrows(QueryPlanException.class, () -> service.generate(conversation, run, ExportFormat.CSV));
        doAnswer(call -> { AnalyticExportSink sink = call.getArgument(3); sink.columns(columns);
            sink.row(Map.of("employee.full_name", "a".repeat(2000))); return null;
        }).when(queries).export(anyString(), anyInt(), anyInt(), any());
        limits.setTabularMaxBytes(1024);
        assertThrows(AiExportLimitException.class, () -> service.generate(conversation, run, ExportFormat.CSV));
        doAnswer(call -> { ((AnalyticExportSink) call.getArgument(3)).columns(Collections.nCopies(9, columns.getFirst())); return null; })
                .when(queries).export(anyString(), anyInt(), anyInt(), any());
        assertThrows(AiExportLimitException.class, () -> service.generate(conversation, run, ExportFormat.PDF));
        doAnswer(call -> { ((AnalyticExportSink) call.getArgument(3)).columns(columns); return null; })
                .when(queries).export(anyString(), anyInt(), anyInt(), any());
        assertThrows(AiExportLimitException.class, () -> service.generate(conversation, run, ExportFormat.XLSX));
        assertEquals(before, temporaryExports());
    }

    @Test void boundsConcurrentDownloadsAndReleasesPermitWhenClosed() {
        limits.setMaxConcurrent(1); service = new AiReportExportService(persistence, queries, security, timezone, limits);
        try (var first = service.generate(conversation, run, ExportFormat.CSV)) {
            assertThrows(AiExportBusyException.class, () -> service.generate(conversation, run, ExportFormat.CSV));
        }
        try (var again = service.generate(conversation, run, ExportFormat.CSV)) { assertTrue(again.size() > 0); }
    }

    @Test void propertiesHaveValidatedFiniteBoundsAndFormatsAreStrict() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator(); assertTrue(validator.validate(limits).isEmpty());
            limits.setTabularMaxRows(0); limits.setPdfMaxColumns(9); limits.setTimeoutSeconds(121);
            assertEquals(3, validator.validate(limits).size());
        }
        assertThrows(BusinessException.class, () -> ExportFormat.parse("PDF"));
        assertThrows(BusinessException.class, () -> ExportFormat.parse("sql"));
    }
    private Set<Path> temporaryExports() throws Exception {
        try (var files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files.filter(path -> path.getFileName().toString().startsWith("hrcore-ai-export-")).collect(java.util.stream.Collectors.toSet());
        }
    }
}
