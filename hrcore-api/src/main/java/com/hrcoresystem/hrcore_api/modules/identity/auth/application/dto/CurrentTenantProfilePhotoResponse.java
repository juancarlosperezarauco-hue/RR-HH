package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;

public record CurrentTenantProfilePhotoResponse(
        byte[] bytes,
        String contentType,
        String filename
) {
}
