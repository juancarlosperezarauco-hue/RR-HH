package com.hrcoresystem.hrcore_api.modules.identity.access.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TenantRole(
        UUID id,
        String name,
        String description,
        boolean active,
        Instant createdAt
) {
}