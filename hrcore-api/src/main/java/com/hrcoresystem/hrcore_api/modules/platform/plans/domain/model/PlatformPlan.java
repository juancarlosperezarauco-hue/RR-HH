package com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlatformPlan(
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
    public PlatformPlan(
            UUID id,
            String code,
            String name,
            String description,
            int maxUsers,
            int maxRoles,
            String planType,
            Integer trialDays,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                code,
                name,
                description,
                maxUsers,
                maxRoles,
                planType,
                trialDays,
                null,
                null,
                "USD",
                true,
                0,
                active,
                createdAt,
                updatedAt
        );
    }
}
