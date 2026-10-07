package com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model;

import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionCheckoutSession(
        UUID id,
        UUID tenantId,
        UUID planId,
        UUID requestedByUserId,
        String requestedByEmail,
        BillingInterval billingInterval,
        String status,
        String stripeCustomerId,
        String stripeSessionId,
        String stripeSubscriptionId,
        String stripePriceId,
        String checkoutUrl,
        String successUrl,
        String cancelUrl,
        BigDecimal amount,
        String currency,
        Instant completedAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
