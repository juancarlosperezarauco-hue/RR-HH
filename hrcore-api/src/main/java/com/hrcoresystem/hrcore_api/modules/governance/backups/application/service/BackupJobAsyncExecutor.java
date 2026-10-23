package com.hrcoresystem.hrcore_api.modules.governance.backups.application.service;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.maintenance.TenantMaintenanceService;
import com.hrcoresystem.hrcore_api.common.tenancy.migration.TenantSchemaMigrationService;
import com.hrcoresystem.hrcore_api.common.tenancy.validation.TenantSchemaReadinessService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.repository.BackupJobRepository;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.engine.BackupEngine;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.BackupStorage;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.ChecksumService;
import com.hrcoresystem.hrcore_api.modules.identity.access.application.service.OwnerPermissionSynchronizer;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Files;
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
    private final ChecksumService checksums;
    private final TenantSchemaMigrationService migrations;
    private final OwnerPermissionSynchronizer ownerPermissions;
    private final TenantSchemaReadinessService readiness;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationPublisherPort notifications;

    public BackupJobAsyncExecutor(
            BackupJobRepository repo,
            BackupEngine engine,
            BackupStorage storage,
            ChecksumService checksums,
            AuditTrailService audit,
            TenantMaintenanceService maintenance,
            TenantSchemaMigrationService migrations,
            OwnerPermissionSynchronizer ownerPermissions,
            TenantSchemaReadinessService readiness,
            @Qualifier("targetDataSource") DataSource targetDataSource,
            NotificationPublisherPort notifications
    ) {
        this.repo = repo;
        this.engine = engine;
        this.storage = storage;
        this.checksums = checksums;
        this.audit = audit;
        this.maintenance = maintenance;
        this.migrations = migrations;
        this.ownerPermissions = ownerPermissions;
        this.readiness = readiness;
        this.jdbcTemplate = new JdbcTemplate(targetDataSource);
        this.notifications = notifications;
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
            notifyTenant(done, NotificationType.SYSTEM_NOTICE, "Respaldo completado", "El respaldo de tu tenant se completó correctamente.", NotificationPriority.NORMAL);
        } catch (Exception e) {
            log.error("Backup job failed: {}", id, e);
            repo.findById(id).ifPresent(job -> {
                BackupJob failed = status(job, BackupStatus.FAILED, e.getMessage(), null, Instant.now());
                record(failed, BackupAuditEventTypes.BACKUP_FAILED, e.getMessage());
                notifyTenant(failed, NotificationType.SYSTEM_NOTICE, "Error en respaldo", "No se pudo completar el respaldo solicitado.", NotificationPriority.HIGH);
            });
        }
    }

    @Async("backupTaskExecutor")
    public void executeRestore(UUID id) {
        BackupJob restore = null;
        try {
            restore = get(id);
            BackupJob source = restore.sourceBackupId() == null
                    ? restore
                    : repo.findById(restore.sourceBackupId()).orElseThrow(() -> new BackupNotFoundException("Backup original no encontrado"));
            BackupJob running = status(restore, BackupStatus.RESTORING, null, Instant.now(), null);
            record(running, BackupAuditEventTypes.RESTORE_STARTED, null);
            Path file = storage.resolvePath(source.storagePath());
            if (!Files.isRegularFile(file)) throw new com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupExecutionException("El archivo del respaldo no existe");
            if (source.checksumSha256() != null && !source.checksumSha256().isBlank()) {
                String actualHash = checksums.sha256(file);
                if (!source.checksumSha256().equalsIgnoreCase(actualHash)) throw new com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupExecutionException("Checksum SHA-256 del respaldo no coincide");
            }
            engine.validateArchive(file, running.scope(), running.schemaName());
            if (running.scope() == BackupScope.TENANT_SCHEMA) restoreTenant(running, file);
            else {
                // Los tenants mantienen FKs hacia public.system_permissions. Deben
                // retirarse antes de que pg_restore --clean limpie el esquema public.
                dropTenantSchemasBeforeFullRestore();
                engine.restoreFullDatabase(file);
            }
            BackupJob done = status(get(id), BackupStatus.RESTORED, null, null, Instant.now());
            record(done, BackupAuditEventTypes.RESTORE_COMPLETED, null);
            notifyTenant(done, NotificationType.SYSTEM_NOTICE, "Restauración completada", "La restauración del respaldo se completó correctamente.", NotificationPriority.HIGH);
            log.info("[RESTORE_UPLOAD] restore completed jobId={}, scope={}, fileName={}", done.id(), done.scope(), done.fileName());
        } catch (Exception e) {
            log.error("Restore job failed: {}", id, e);
            repo.findById(id).ifPresent(job -> {
                String failureReason = safeMessage(e);
                BackupJob failed = status(job, BackupStatus.RESTORE_FAILED, failureReason, null, Instant.now());
                record(failed, BackupAuditEventTypes.RESTORE_FAILED, failureReason);
                notifyTenant(failed, NotificationType.SYSTEM_NOTICE, "Error en restauración", "No se pudo completar la restauración del respaldo.", NotificationPriority.CRITICAL);
            });
        }
    }

    private void restoreTenant(BackupJob running, Path file) {
        maintenance.enableMaintenance(running.tenantSlug(), "Restaurando backup " + running.id());
        engine.restoreSchema(running.schemaName(), file);
        migrations.migrateSchema(running.schemaName());
        ownerPermissions.synchronizeTenant(running.schemaName());
        readiness.assertTenantOperational(running.schemaName(), running.tenantSlug());
        maintenance.disableMaintenance(running.tenantSlug());
    }

    private void dropTenantSchemasBeforeFullRestore() {
        List<String> schemas = jdbcTemplate.queryForList(
                "SELECT schema_name FROM information_schema.schemata "
                        + "WHERE schema_name LIKE 'tenant\\_%' ESCAPE '\\'",
                String.class
        );
        for (String schema : schemas) {
            if (schema != null && schema.matches("^[a-zA-Z0-9_]+$")) {
                jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
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

    private void notifyTenant(BackupJob job, NotificationType type, String title, String body, NotificationPriority priority) {
        if (job == null || job.scope() == BackupScope.FULL_DATABASE || job.tenantSlug() == null || job.schemaName() == null) return;
        UUID userId;
        try { userId = UUID.fromString(job.requestedBy()); } catch (Exception ex) { return; }
        TenantContext previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(new TenantContext(job.tenantSlug(), job.schemaName(), false));
            notifications.publish(new NotificationPublishRequest(userId, type, NotificationCategory.SYSTEM, priority,
                    title, body, null, null, "/dashboard/backups?id=" + job.id(), null));
        } catch (Exception ex) {
            log.warn("Unable to publish backup notification. jobId={}", job.id(), ex);
        } finally {
            if (previous == null) TenantContextHolder.clear(); else TenantContextHolder.set(previous);
        }
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
