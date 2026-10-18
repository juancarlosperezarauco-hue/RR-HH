package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
public record EmployeeTermination(UUID id, UUID employeeId, UUID employmentPeriodId, LocalDate effectiveDate, String reason,
        UUID performedBy, Instant createdAt) { }
