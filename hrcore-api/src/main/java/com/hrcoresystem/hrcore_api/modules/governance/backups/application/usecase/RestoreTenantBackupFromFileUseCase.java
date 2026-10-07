package com.hrcoresystem.hrcore_api.modules.governance.backups.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto.BackupJobResponse;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.mapper.BackupJobMapper;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.service.BackupApplicationService;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.exception.BackupOperationNotAllowedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RestoreTenantBackupFromFileUseCase {

    private static final String CONFIRMATION = "RESTORE_TENANT_BACKUP";

    private final BackupApplicationService backupApplicationService;
    private final BackupJobMapper backupJobMapper;

    public RestoreTenantBackupFromFileUseCase(
            BackupApplicationService backupApplicationService,
            BackupJobMapper backupJobMapper
    ) {
        this.backupApplicationService = backupApplicationService;
        this.backupJobMapper = backupJobMapper;
    }

    @Transactional
    public BackupJobResponse execute(MultipartFile file, String confirmationText, String reason) {
        String normalizedConfirmation = confirmationText == null ? "" : confirmationText.trim();

        if (!CONFIRMATION.equals(normalizedConfirmation)) {
            throw new BackupOperationNotAllowedException("Invalid confirmation text. Expected: " + CONFIRMATION);
        }

        return backupJobMapper.toResponse(
                backupApplicationService.createTenantRestoreJobFromFile(file, reason)
        );
    }
}
