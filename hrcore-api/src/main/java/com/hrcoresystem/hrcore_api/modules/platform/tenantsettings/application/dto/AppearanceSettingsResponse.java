package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;

public record AppearanceSettingsResponse(
        String preset,
        String primary,
        String secondary,
        String sidebar,
        String mode
) {
}
