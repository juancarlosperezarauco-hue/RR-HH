package com.hrcoresystem.hrcore_api.modules.governance.backups.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto.BackupJobResponse;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.mapper.BackupJobMapper;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.service.BackupApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTenantBackupUseCase {

    private final BackupApplicationService backupApplicationService;
    private final BackupJobMapper backupJobMapper;

    public CreateTenantBackupUseCase(BackupApplicationService backupApplicationService, BackupJobMapper backupJobMapper) {
        this.backupApplicationService = backupApplicationService;
        this.backupJobMapper = backupJobMapper;
    }

    @Transactional
    public BackupJobResponse execute(String reason) {
        return backupJobMapper.toResponse(backupApplicationService.createTenantBackup(reason));
    }
}
