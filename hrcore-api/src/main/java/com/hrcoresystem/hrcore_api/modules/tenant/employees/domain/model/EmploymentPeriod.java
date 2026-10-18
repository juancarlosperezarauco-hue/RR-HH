package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmploymentPeriod(UUID id, UUID employeeId, Integer periodNumber,
        LocalDate startDate, LocalDate endDate, EmploymentStatus status,
        Instant createdAt, Instant updatedAt) { }
