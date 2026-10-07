package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantSettingsRequest(
        @NotBlank
        @Size(max = 150)
        String companyName,

        @Size(max = 200)
        String legalName,

        @NotBlank
        @Size(max = 100)
        String timezone,

        @NotBlank
        @Size(max = 10)
        String currency,

        @Email
        @Size(max = 150)
        String contactEmail,

        @Size(max = 50)
        String contactPhone
) {
}