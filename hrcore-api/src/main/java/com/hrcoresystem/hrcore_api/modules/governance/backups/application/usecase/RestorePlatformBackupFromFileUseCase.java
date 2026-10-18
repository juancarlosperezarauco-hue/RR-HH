package com.hrcoresystem.hrcore_api.modules.governance.backups.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto.BackupJobResponse;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.mapper.BackupJobMapper;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.service.BackupApplicationService;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupOperationNotAllowedException;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class RestorePlatformBackupFromFileUseCase {

    private static final String CONFIRMATION_TENANT = "RESTORE_PLATFORM_BACKUP";
    private static final String CONFIRMATION_FULL = "RESTORE_FULL_DATABASE";

    private final BackupApplicationService backupApplicationService;
    private final BackupJobMapper backupJobMapper;

    public RestorePlatformBackupFromFileUseCase(
            BackupApplicationService backupApplicationService,
            BackupJobMapper backupJobMapper
    ) {
        this.backupApplicationService = backupApplicationService;
        this.backupJobMapper = backupJobMapper;
    }

    @Transactional
    public BackupJobResponse execute(
            MultipartFile file,
            BackupScope scope,
            UUID tenantId,
            String confirmationText,
            String reason
    ) {
        String normalizedConfirmation = confirmationText == null ? "" : confirmationText.trim();

        if (scope == null) {
            throw new BackupOperationNotAllowedException("scope es obligatorio");
        }
        String expected = scope == BackupScope.FULL_DATABASE ? CONFIRMATION_FULL : CONFIRMATION_TENANT;
        if (!expected.equals(normalizedConfirmation)) {
            throw new BackupOperationNotAllowedException("Confirmación inválida. Se esperaba: " + expected);
        }
        if (scope == BackupScope.TENANT_SCHEMA && tenantId == null) {
            throw new BackupOperationNotAllowedException("tenantId es obligatorio para TENANT_SCHEMA");
        }
        if (scope == BackupScope.FULL_DATABASE && tenantId != null) {
            throw new BackupOperationNotAllowedException("tenantId no se permite para FULL_DATABASE");
        }

        return backupJobMapper.toResponse(
                backupApplicationService.createPlatformRestoreJobFromFile(scope, tenantId, file, reason)
        );
    }
}
