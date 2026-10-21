package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAppearanceSettingsRequest(
        @NotBlank @Pattern(regexp = "original|corporate|modern|natural|minimal|warm") String preset,
        @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String primary,
        @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String secondary,
        @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String sidebar,
        @NotBlank @Pattern(regexp = "light|dark") String mode
) {
}
