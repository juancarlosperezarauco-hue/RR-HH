package com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto;

import jakarta.validation.constraints.Size;

public record CreateBackupRequest(@Size(max = 500)
        String reason) {

}
