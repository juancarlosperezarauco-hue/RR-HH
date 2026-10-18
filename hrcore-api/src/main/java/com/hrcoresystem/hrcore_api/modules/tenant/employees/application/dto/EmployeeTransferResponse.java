package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;
import java.time.LocalDate;
import java.util.UUID;
public record EmployeeTransferResponse(UUID employeeId, EmployeeAssignmentResponse previousAssignment,
        EmployeeAssignmentResponse newAssignment, LocalDate effectiveDate, String reason) { }
