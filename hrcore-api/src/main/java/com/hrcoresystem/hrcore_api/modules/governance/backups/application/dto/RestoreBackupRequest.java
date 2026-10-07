package com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestoreBackupRequest(
        @NotBlank
        String confirmationText,
        @Size(max = 500)
        String reason
        ) {

}
