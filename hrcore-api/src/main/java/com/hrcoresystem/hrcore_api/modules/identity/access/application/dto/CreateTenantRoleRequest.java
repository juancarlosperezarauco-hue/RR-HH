package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTenantRoleRequest(
        @Size(max = 100)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        List<String> permissionCodes
) {
    public CreateTenantRoleRequest(String name, String description, List<String> permissionCodes) {
        this(null, name, description, permissionCodes);
    }
}
