package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateDepartmentRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        UUID parentDepartmentId,
        UUID managerEmployeeId,
        @Size(max = 50) String costCenterCode,
        String reason
) { }
