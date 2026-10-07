package com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCheckoutSessionRequest(
        @NotBlank
        String planCode,

        @NotBlank
        String billingInterval
) {
}
