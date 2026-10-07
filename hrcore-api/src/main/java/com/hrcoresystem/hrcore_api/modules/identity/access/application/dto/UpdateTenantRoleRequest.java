package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateTenantRoleRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        List<String> permissionCodes
) {
}