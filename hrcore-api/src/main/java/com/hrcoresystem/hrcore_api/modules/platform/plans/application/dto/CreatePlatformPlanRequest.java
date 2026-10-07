package com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePlatformPlanRequest(
        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        @Min(1)
        int maxUsers,

        @Min(1)
        int maxRoles,

        @NotBlank
        @Size(max = 20)
        String planType,

        Integer trialDays,

        BigDecimal monthlyAmount,
        BigDecimal yearlyAmount,

        @Size(max = 10)
        String currency,

        Boolean publicVisible,

        Integer sortOrder
) {
}
