package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EmployeeOnboardingRequest(
        @NotNull @Valid EmployeeRequest employee,
        @NotNull @Valid EmployeeAssignmentRequest assignment
) { }
