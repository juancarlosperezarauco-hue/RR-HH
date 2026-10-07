package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PlatformSubscriptionResponse(
        UUID id,
        UUID tenantId,
        String tenantName,
        String tenantSlug,
        UUID planId,
        String planCode,
        String planName,
        String planType,
        int maxUsers,
        int maxRoles,
        String status,
        boolean trial,
        boolean currentSubscription,
        String stripeSubscriptionId,
        String stripePriceId,
        String billingInterval,
        Instant startedAt,
        Instant expiresAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant cancelledAt,
        Long remainingDays,
        Instant createdAt,
        Instant updatedAt
) {
}
