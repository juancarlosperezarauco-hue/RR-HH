package com.hrcoresystem.hrcore_api.modules.identity.access.application.dto;

public record SystemPermissionResponse(
        String code,
        String module,
        String description
) {
}