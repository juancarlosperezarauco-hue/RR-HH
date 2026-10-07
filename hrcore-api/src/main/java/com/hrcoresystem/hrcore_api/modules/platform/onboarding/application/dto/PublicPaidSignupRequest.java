package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicPaidSignupRequest(
        @NotBlank
        @Size(max = 150)
        String companyName,

        @NotBlank
        @Size(max = 100)
        String tenantSlug,

        @NotBlank
        @Email
        @Size(max = 150)
        String adminEmail,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @NotBlank
        @Size(max = 50)
        String planCode,

        @NotBlank
        @Size(max = 20)
        String billingInterval
) {
}
