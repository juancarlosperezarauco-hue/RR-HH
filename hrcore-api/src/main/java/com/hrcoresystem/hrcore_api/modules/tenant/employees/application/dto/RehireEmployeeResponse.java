package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;

public record RehireEmployeeResponse(EmployeeResponse employee, EmploymentPeriodResponse employmentPeriod,
                                     EmployeeAssignmentResponse assignment) { }
