package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record BillingCheckoutResultResponse(
        UUID checkoutSessionId,
        String stripeSessionId,
        String status,
        String planCode,
        String billingInterval,
        String checkoutUrl,
        Instant completedAt,
        Instant expiresAt
) {
}
