package com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model;

import java.time.Instant;
import java.util.UUID;

public record BackupJob(
        UUID id,
        BackupOperationType operationType,
        BackupScope scope,
        BackupStatus status,
        UUID tenantId,
        String tenantSlug,
        String schemaName,
        UUID sourceBackupId,
        UUID preRestoreBackupId,
        String fileName,
        String storagePath,
        BackupFormat format,
        Long sizeBytes,
        String checksumSha256,
        String requestedBy,
        String reason,
        String failureReason,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt
        ) {

}
