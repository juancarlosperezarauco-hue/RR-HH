package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PlatformTenantResponse(
        UUID id,
        String name,
        String slug,
        String schemaName,
        String status,
        UUID planId,
        String planName,
        String planCode,
        String subscriptionStatus,
        String legalName,
        String taxIdentifier,
        String contactEmail,
        String contactPhone,
        String ownerName,
        String ownerEmail,
        String stripeCustomerId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public PlatformTenantResponse(
            UUID id,
            String name,
            String slug,
            String schemaName,
            String status,
            UUID planId,
            String stripeCustomerId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, name, slug, schemaName, status, planId, null, null, null, null, null, null, null, null, null, stripeCustomerId, active, createdAt, updatedAt);
    }
}
