package com.hrcoresystem.hrcore_api.modules.governance.backups.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class BackupJobFactory {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    private BackupJobFactory() {
    }

    public static BackupJob tenantBackup(UUID tenantId, String tenantSlug, String schemaName, String requestedBy, String reason) {
        String file = schemaName + "_" + F.format(Instant.now()) + ".dump";
        return new BackupJob(null, BackupOperationType.BACKUP, BackupScope.TENANT_SCHEMA, BackupStatus.PENDING, tenantId, tenantSlug, schemaName, null, null, file, file, BackupFormat.CUSTOM, null, null, requestedBy, reason, null, null, null, null, null);
    }

    public static BackupJob fullBackup(String requestedBy, String reason) {
        String file = "full_database_" + F.format(Instant.now()) + ".dump";
        return new BackupJob(null, BackupOperationType.BACKUP, BackupScope.FULL_DATABASE, BackupStatus.PENDING, null, null, null, null, null, file, file, BackupFormat.CUSTOM, null, null, requestedBy, reason, null, null, null, null, null);
    }

    public static BackupJob restoreJobFromBackup(BackupJob source, UUID preRestoreBackupId, String requestedBy, String reason) {
        String file = "restore_" + source.id() + "_" + F.format(Instant.now()) + ".job";
        return new BackupJob(null, BackupOperationType.RESTORE, source.scope(), BackupStatus.PENDING, source.tenantId(), source.tenantSlug(), source.schemaName(), source.id(), preRestoreBackupId, file, file, source.format(), null, null, requestedBy, reason, null, null, null, null, null);
    }

    public static BackupJob restoreJobFromUploadedFile(
            BackupScope scope,
            UUID tenantId,
            String tenantSlug,
            String schemaName,
            String fileName,
            String storagePath,
            BackupFormat format,
            String requestedBy,
            String reason
    ) {
        return new BackupJob(
                null,
                BackupOperationType.RESTORE,
                scope,
                BackupStatus.PENDING,
                tenantId,
                tenantSlug,
                schemaName,
                null,
                null,
                fileName,
                storagePath,
                format,
                null,
                null,
                requestedBy,
                reason,
                null,
                null,
                null,
                null,
                null
        );
    }
}
