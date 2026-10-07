package com.hrcoresystem.hrcore_api.modules.governance.backups.application.service;

import com.hrcoresystem.hrcore_api.common.tenancy.maintenance.TenantMaintenanceService;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.repository.BackupJobRepository;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.engine.BackupEngine;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.BackupStorage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
public class BackupJobAsyncExecutor {

    private static final Logger log = LoggerFactory.getLogger(BackupJobAsyncExecutor.class);
    private final BackupJobRepository repo;
    private final BackupEngine engine;
    private final BackupStorage storage;
    private final AuditTrailService audit;
    private final TenantMaintenanceService maintenance;
    private final JdbcTemplate jdbcTemplate;

    public BackupJobAsyncExecutor(
            BackupJobRepository repo,
            BackupEngine engine,
            BackupStorage storage,
            AuditTrailService audit,
            TenantMaintenanceService maintenance,
            @Qualifier("targetDataSource") DataSource targetDataSource
    ) {
        this.repo = repo;
        this.engine = engine;
        this.storage = storage;
        this.audit = audit;
        this.maintenance = maintenance;
        this.jdbcTemplate = new JdbcTemplate(targetDataSource);
    }

    @Async("backupTaskExecutor")
    public void executeBackup(UUID id) {
        try {
            BackupJob job = get(id);
            BackupJob running = status(job, BackupStatus.RUNNING, null, Instant.now(), null);
            Path target = storage.resolveTargetPath(running.fileName());
            BackupArtifact a = running.scope() == BackupScope.FULL_DATABASE ? engine.dumpFullDatabase(target) : engine.dumpSchema(running.schemaName(), target);
            BackupJob done = new BackupJob(running.id(), running.operationType(), running.scope(), BackupStatus.COMPLETED, running.tenantId(), running.tenantSlug(), running.schemaName(), running.sourceBackupId(), running.preRestoreBackupId(), running.fileName(), storage.toStoragePath(a.path()), running.format(), a.sizeBytes(), a.checksumSha256(), running.requestedBy(), running.reason(), null, running.startedAt(), Instant.now(), running.createdAt(), running.updatedAt());
            repo.save(done);
            record(done, BackupAuditEventTypes.BACKUP_COMPLETED, null);
        } catch (Exception e) {
            log.error("Backup job failed: {}", id, e);
            repo.findById(id).ifPresent(job -> {
                BackupJob failed = status(job, BackupStatus.FAILED, e.getMessage(), null, Instant.now());
                record(failed, BackupAuditEventTypes.BACKUP_FAILED, e.getMessage());
            });
        }
    }

    @Async("backupTaskExecutor")
    public void executeRestore(UUID id) {
        BackupJob restore = null;
        try {
            restore = get(id);
            BackupJob source = restore.sourceBackupId() != null
                    ? repo.findById(restore.sourceBackupId())
                        .orElseThrow(() -> new BackupNotFoundException("Source backup not found"))
                    : restore;

            BackupJob running = status(restore, BackupStatus.RESTORING, null, Instant.now(), null);
            record(running, BackupAuditEventTypes.RESTORE_STARTED, null);
            Path src = storage.resolvePath(source.storagePath());
            String restoreOutput = null;
            Exception postRestoreIssue = null;

            if (running.scope() == BackupScope.TENANT_SCHEMA) {
                maintenance.enableMaintenance(running.tenantSlug(), "Restoring backup " + source.id());
                try {
                    restoreOutput = engine.restoreSchema(running.schemaName(), src);
                } finally {
                    try {
                        maintenance.disableMaintenance(running.tenantSlug());
                    } catch (Exception maintenanceError) {
                        log.warn(
                                "Tenant restore applied, but maintenance disable failed. tenantSlug={}, error={}",
                                running.tenantSlug(),
                                maintenanceError.getMessage(),
                                maintenanceError
                        );
                        postRestoreIssue = maintenanceError;
                    }
                }

            } else {
                prepareForFullDatabaseRestore();
                restoreOutput = engine.restoreFullDatabase(src);

            }

            boolean hasWarnings = restoreOutput != null || postRestoreIssue != null;
            String warningMessage = buildWarningMessage(restoreOutput, postRestoreIssue);

            BackupJob done = status(
                    get(id),
                    hasWarnings ? BackupStatus.RESTORED_WITH_WARNINGS : BackupStatus.RESTORED,
                    warningMessage,
                    null,
                    Instant.now()
            );
            record(done, BackupAuditEventTypes.RESTORE_COMPLETED, warningMessage);
        } catch (Exception e) {
            log.error("Restore job failed: {}", id, e);
            if (restore != null && restore.tenantSlug() != null) {
                try {
                    maintenance.disableMaintenance(restore.tenantSlug());
                } catch (Exception maintenanceError) {
                    log.warn("Unable to disable maintenance after restore failure for tenant '{}': {}", restore.tenantSlug(), maintenanceError.getMessage(), maintenanceError);
                }
            }
            repo.findById(id).ifPresent(job -> {
                String failureReason = safeMessage(e);
                BackupJob failed = status(job, BackupStatus.RESTORE_FAILED, failureReason, null, Instant.now());
                record(failed, BackupAuditEventTypes.RESTORE_FAILED, failureReason);
            });
        }
    }

    private BackupJob get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new BackupNotFoundException("Backup job not found with id: " + id));
    }

    private BackupJob status(BackupJob j, BackupStatus s, String fail, Instant started, Instant finished) {
        return repo.save(new BackupJob(j.id(), j.operationType(), j.scope(), s, j.tenantId(), j.tenantSlug(), j.schemaName(), j.sourceBackupId(), j.preRestoreBackupId(), j.fileName(), j.storagePath(), j.format(), j.sizeBytes(), j.checksumSha256(), j.requestedBy(), j.reason(), fail, started != null ? started : j.startedAt(), finished != null ? finished : j.finishedAt(), j.createdAt(), j.updatedAt()));
    }

    private void record(BackupJob j, String type, String error) {
        Map<String, Object> d = Map.of("operationType", j.operationType().name(), "scope", j.scope().name(), "tenantSlug", String.valueOf(j.tenantSlug()), "schemaName", String.valueOf(j.schemaName()), "status", j.status().name(), "fileName", j.fileName(), "error", String.valueOf(error));
        try {
            if (j.scope() == BackupScope.FULL_DATABASE) {
                audit.recordPlatformEvent(type, "BACKUP", j.id().toString(), d);
            } else {
                recordTenantEvent(j, type, d);
            }
        } catch (Exception e) {
            log.warn("Unable to record backup audit event. jobId={}, eventType={}", j.id(), type, e);
        }
    }

    private void recordTenantEvent(BackupJob j, String type, Map<String, Object> details) {
        TenantContext previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(new TenantContext(j.tenantSlug(), j.schemaName(), false));
            audit.recordTenantEvent(type, "BACKUP", j.id().toString(), details);
        } finally {
            if (previous == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.set(previous);
            }
        }
    }

    private void prepareForFullDatabaseRestore() {
        dropBackupJobsTenantForeignKey();

        List<String> tenantSchemas = jdbcTemplate.queryForList(
                """
                SELECT schema_name
                FROM public.platform_tenants
                WHERE schema_name IS NOT NULL
                  AND schema_name LIKE 'tenant\\_%' ESCAPE '\\'
                """,
                String.class
        );

        for (String schemaName : tenantSchemas) {
            dropSchema(schemaName);
        }
    }

    private void dropBackupJobsTenantForeignKey() {
        jdbcTemplate.execute(
                """
                ALTER TABLE public.backup_jobs
                    DROP CONSTRAINT IF EXISTS backup_jobs_tenant_id_fkey
                """
        );
    }

    private void dropSchema(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-zA-Z0-9_]+$")) {
            log.warn("Skipping invalid tenant schema name during full restore cleanup: {}", schemaName);
            return;
        }

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schemaName + " CASCADE");
    }

    private String buildWarningMessage(String restoreOutput, Exception postRestoreIssue) {
        StringBuilder builder = new StringBuilder();

        if (restoreOutput != null) {
            builder.append("Restore completed with pg_restore warnings.");

            if (!restoreOutput.isBlank()) {
                builder.append(" Output: ").append(restoreOutput);
            }
        }

        if (postRestoreIssue != null) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }

            builder.append("Post-restore task warning: ")
                    .append(safeMessage(postRestoreIssue));
        }

        if (builder.length() == 0) {
            return null;
        }

        String value = builder.toString();
        if (value.length() <= 3500) {
            return value;
        }

        return value.substring(0, 3500) + "... [truncated]";
    }

    private String safeMessage(Exception exception) {
        if (exception == null) {
            return null;
        }

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message.length() <= 3500
                ? message
                : message.substring(0, 3500) + "... [truncated]";
    }
}
