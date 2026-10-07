package com.hrcoresystem.hrcore_api.modules.identity.auth.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CurrentTenantProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active,
        String status,
        String tenantSlug,
        boolean faceLoginEnabled,
        boolean profilePhotoAvailable,
        String profilePhotoUrl,
        String profilePhotoContentType,
        Instant createdAt,
        Instant updatedAt
) {
}
