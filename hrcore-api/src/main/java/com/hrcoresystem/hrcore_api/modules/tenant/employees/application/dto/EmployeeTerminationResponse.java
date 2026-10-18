package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;
import java.time.LocalDate;
public record EmployeeTerminationResponse(EmployeeResponse employee, LocalDate effectiveDate, String reason,
        int terminatedContracts, int cancelledContracts, int closedAssignments, int cancelledAssignments,
        int closedShiftAssignments, int cancelledShiftAssignments, boolean linkedUserDeactivated) { }
