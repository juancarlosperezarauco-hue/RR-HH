package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillingPlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        String planType,
        Integer trialDays,
        BigDecimal monthlyAmount,
        BigDecimal yearlyAmount,
        String currency,
        boolean publicVisible,
        int sortOrder,
        boolean active,
        boolean currentPlan,
        boolean availableForCheckout
) {
}
