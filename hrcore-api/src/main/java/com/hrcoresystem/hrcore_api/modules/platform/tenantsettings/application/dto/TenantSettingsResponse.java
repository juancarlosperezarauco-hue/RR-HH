package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;

public record TenantSettingsResponse(
        String companyName,
        String legalName,
        String timezone,
        String currency,
        String contactEmail,
        String contactPhone
) {
}