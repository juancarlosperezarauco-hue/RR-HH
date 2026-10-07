package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicSignupRequest(
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
        @NotBlank
        @Size(max = 100)
        String firstName,
        @NotBlank
        @Size(max = 100)
        String lastName
        ) {

}
