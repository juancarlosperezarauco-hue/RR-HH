package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantLanguageRequest(@NotBlank String language) {
}
