package com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Department(
        UUID id, String code, String name, String description,
        UUID parentDepartmentId, UUID managerEmployeeId, String costCenterCode,
        DepartmentStatus status, UUID createdBy, UUID updatedBy,
        Instant createdAt, Instant updatedAt
) { }
