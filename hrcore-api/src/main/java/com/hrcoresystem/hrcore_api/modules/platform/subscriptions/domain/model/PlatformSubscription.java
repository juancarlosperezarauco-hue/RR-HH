package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PlatformSubscription(
        UUID id,
        UUID tenantId,
        UUID planId,
        PlatformSubscriptionStatus status,
        boolean trial,
        boolean currentSubscription,
        String stripeSubscriptionId,
        String stripePriceId,
        BillingInterval billingInterval,
        Instant startedAt,
        Instant expiresAt,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
    public PlatformSubscription(
            UUID id,
            UUID tenantId,
            UUID planId,
            PlatformSubscriptionStatus status,
            boolean trial,
            boolean currentSubscription,
            Instant startedAt,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                tenantId,
                planId,
                status,
                trial,
                currentSubscription,
                null,
                null,
                null,
                startedAt,
                expiresAt,
                null,
                null,
                false,
                null,
                createdAt,
                updatedAt
        );
    }
}
