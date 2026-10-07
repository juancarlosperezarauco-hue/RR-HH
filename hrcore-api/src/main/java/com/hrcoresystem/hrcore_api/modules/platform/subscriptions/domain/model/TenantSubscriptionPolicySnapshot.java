package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TenantSubscriptionPolicySnapshot(
        UUID tenantId,
        String tenantSlug,
        boolean tenantActive,
        String tenantStatus,
        UUID subscriptionId,
        PlatformSubscriptionStatus subscriptionStatus,
        boolean trial,
        Instant expiresAt,
        UUID planId,
        String planCode,
        String planType,
        int maxUsers,
        int maxRoles
        ) {

}
