package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.FileStatus;
import java.time.Instant;
import java.util.UUID;

public record FileResponse(
    UUID id,
    UUID employeeId,
    String fileNumber,
    FileStatus status,
    String notes,
    Instant createdAt,
    Instant updatedAt) {}
