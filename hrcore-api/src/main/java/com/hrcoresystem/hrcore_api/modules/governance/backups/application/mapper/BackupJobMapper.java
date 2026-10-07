package com.hrcoresystem.hrcore_api.modules.governance.backups.application.mapper;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto.BackupJobResponse;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupJob;
import org.springframework.stereotype.Component;

@Component
public class BackupJobMapper {

    public BackupJobResponse toResponse(BackupJob b) {
        return new BackupJobResponse(b.id(), b.operationType().name(), b.scope().name(), b.status().name(),
                b.tenantId(), b.tenantSlug(), b.schemaName(), b.sourceBackupId(), b.preRestoreBackupId(),
                b.fileName(), b.format().name(), b.sizeBytes(), b.checksumSha256(), b.requestedBy(),
                b.reason(), b.failureReason(), b.startedAt(), b.finishedAt(), b.createdAt(), b.updatedAt());
    }
}
