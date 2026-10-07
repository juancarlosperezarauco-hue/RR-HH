package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TenantRoleResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        List<String> permissionCodes
) {
}