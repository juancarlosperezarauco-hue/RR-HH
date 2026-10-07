package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicSignupResponse(
        UUID tenantId,
        String tenantSlug,
        String companyName,
        String adminEmail,
        String initialRole,
        String currentPlanCode,
        String subscriptionStatus,
        Instant trialExpiresAt,
        String loginHint
        ) {

}
