package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusRequest(@NotNull EmploymentStatus status) { }
