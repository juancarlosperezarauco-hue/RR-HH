package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateDepartmentRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        UUID parentDepartmentId,
        UUID managerEmployeeId,
        @Size(max = 50) String costCenterCode,
        String reason
) { public UpdateDepartmentRequest(String ignoredCode,String name,String description,UUID parentDepartmentId,UUID managerEmployeeId,String costCenterCode,String reason){this(name,description,parentDepartmentId,managerEmployeeId,costCenterCode,reason);} }
