package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignPlatformSubscriptionRequest(
        @NotNull
        UUID tenantId,

        @NotBlank
        String planCode,

        Integer overrideTrialDays
) {
}
