package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
public record EmployeeTerminationHistoryResponse(UUID employeeId, UUID employmentPeriodId, LocalDate effectiveDate, String reason,
        UUID performedBy, Instant createdAt) { }
