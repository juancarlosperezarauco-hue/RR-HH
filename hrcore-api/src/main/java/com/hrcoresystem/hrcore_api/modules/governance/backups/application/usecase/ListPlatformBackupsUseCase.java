package com.hrcoresystem.hrcore_api.modules.governance.backups.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto.BackupJobResponse;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.mapper.BackupJobMapper;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.service.BackupApplicationService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ListPlatformBackupsUseCase {

    private final BackupApplicationService backupApplicationService;
    private final BackupJobMapper backupJobMapper;

    public ListPlatformBackupsUseCase(BackupApplicationService backupApplicationService, BackupJobMapper backupJobMapper) {
        this.backupApplicationService = backupApplicationService;
        this.backupJobMapper = backupJobMapper;
    }

    public List<BackupJobResponse> execute() {
        return backupApplicationService.listPlatformBackups().stream().map(backupJobMapper::toResponse).toList();
    }
}
