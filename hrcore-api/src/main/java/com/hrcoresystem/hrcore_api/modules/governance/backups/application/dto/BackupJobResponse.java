package com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto;

import java.time.Instant;
import java.util.UUID;

public record BackupJobResponse(
        UUID id,
        String operationType,
        String scope,
        String status,
        UUID tenantId,
        String tenantSlug,
        String schemaName,
        UUID sourceBackupId,
        UUID preRestoreBackupId,
        String fileName,
        String format,
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
