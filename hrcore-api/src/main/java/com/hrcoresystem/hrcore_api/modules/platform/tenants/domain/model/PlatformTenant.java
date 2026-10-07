package com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PlatformTenant(
        UUID id,
        String name,
        String slug,
        String schemaName,
        PlatformTenantStatus status,
        UUID planId,
        String stripeCustomerId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public PlatformTenant(
            UUID id,
            String name,
            String slug,
            String schemaName,
            PlatformTenantStatus status,
            UUID planId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, name, slug, schemaName, status, planId, null, active, createdAt, updatedAt);
    }
}
