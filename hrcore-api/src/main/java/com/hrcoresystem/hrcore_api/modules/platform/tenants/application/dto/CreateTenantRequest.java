package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 100)
        String slug,

        @Size(max = 50)
        String planCode
) {
}
