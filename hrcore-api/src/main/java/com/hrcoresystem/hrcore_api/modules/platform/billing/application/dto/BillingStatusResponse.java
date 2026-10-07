package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

import java.time.Instant;
import java.util.UUID;

public record BillingStatusResponse(
        UUID tenantId,
        String tenantSlug,
        String planCode,
        String planName,
        String status,
        String billingInterval,
        boolean trial,
        boolean active,
        boolean pastDue,
        boolean suspended,
        boolean incomplete,
        boolean cancelled,
        boolean cancelAtPeriodEnd,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Instant expiresAt,
        Long remainingDays,
        boolean canUsePlatform,
        boolean canManageBilling,
        boolean canOpenCustomerPortal
) {
}
