package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

public record EmployeeOnboardingResponse(
        EmployeeResponse employee,
        EmployeeAssignmentResponse assignment
) { }
