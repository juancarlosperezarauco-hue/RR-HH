package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicCheckoutActivationStatusResponse(
        UUID checkoutSessionId,
        String stripeSessionId,
        UUID tenantId,
        String tenantSlug,
        String planCode,
        String billingInterval,
        String checkoutStatus,
        String activationStatus,
        boolean paid,
        boolean active,
        boolean failed,
        boolean pending,
        String message,
        Instant expiresAt
) {
}
