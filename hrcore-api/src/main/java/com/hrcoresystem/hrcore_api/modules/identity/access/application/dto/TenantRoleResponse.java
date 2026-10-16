package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRoleType;

public record TenantRoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        TenantRoleType roleType,
        boolean systemRole,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<String> permissionCodes
) {
}
