package com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlatformPlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        int maxUsers,
        int maxRoles,
        String planType,
        Integer trialDays,
        BigDecimal monthlyAmount,
        BigDecimal yearlyAmount,
        String currency,
        boolean publicVisible,
        int sortOrder,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
