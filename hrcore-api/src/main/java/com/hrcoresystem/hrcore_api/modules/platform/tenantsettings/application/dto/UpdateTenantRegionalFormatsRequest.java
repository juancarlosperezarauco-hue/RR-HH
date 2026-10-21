package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantRegionalFormatsRequest(
        @NotBlank String dateFormat,
        @NotBlank String numberFormat
) {
}
