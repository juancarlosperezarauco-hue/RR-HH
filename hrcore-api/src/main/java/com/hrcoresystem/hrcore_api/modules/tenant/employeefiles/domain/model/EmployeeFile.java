package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EmployeeFile(
    UUID id,
    UUID employeeId,
    String fileNumber,
    FileStatus status,
    String notes,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt) {}
