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
        String legalName,
        String taxIdentifier,
        String contactEmail,
        String contactPhone,
        String ownerName,
        String ownerEmail,
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
            String stripeCustomerId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, name, slug, schemaName, status, planId, stripeCustomerId, active, null, null, null, null, null, null, createdAt, updatedAt);
    }

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
        this(id, name, slug, schemaName, status, planId, null, active, null, null, null, null, null, null, createdAt, updatedAt);
    }
}
