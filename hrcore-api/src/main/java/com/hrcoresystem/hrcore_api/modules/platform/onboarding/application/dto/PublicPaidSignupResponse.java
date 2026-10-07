package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicPaidSignupResponse(
        UUID tenantId,
        String tenantSlug,
        String companyName,
        String adminEmail,
        String ownerRole,
        String initialPlanCode,
        String selectedPlanCode,
        String billingInterval,
        String checkoutSessionId,
        String checkoutUrl,
        String checkoutStatus,
        Instant checkoutExpiresAt,
        String message
) {
}
