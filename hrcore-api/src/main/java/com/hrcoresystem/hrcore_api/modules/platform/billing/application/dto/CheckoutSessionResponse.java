package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckoutSessionResponse(
        UUID id,
        String stripeSessionId,
        String checkoutUrl,
        String status,
        String planCode,
        String billingInterval,
        Instant expiresAt
) {
}
