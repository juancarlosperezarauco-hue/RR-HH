package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateFileRequest(
    @NotNull UUID employeeId, @NotBlank @Size(max = 80) String fileNumber, String notes) {}
