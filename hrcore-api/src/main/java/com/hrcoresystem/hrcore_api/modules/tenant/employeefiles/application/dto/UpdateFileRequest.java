package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.FileStatus;
import jakarta.validation.constraints.*;

public record UpdateFileRequest(
    @NotBlank @Size(max = 80) String fileNumber, String notes, @NotNull FileStatus status) {}
