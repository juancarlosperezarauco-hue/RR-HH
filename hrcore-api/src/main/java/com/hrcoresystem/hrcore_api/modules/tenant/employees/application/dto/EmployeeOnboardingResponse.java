package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

public record EmployeeOnboardingResponse(
        EmployeeResponse employee,
        EmployeeAssignmentResponse assignment,
        com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.FileResponse employeeFile
) { }
