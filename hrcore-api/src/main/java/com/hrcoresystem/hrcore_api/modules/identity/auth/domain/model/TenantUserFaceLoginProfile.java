package com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TenantUserFaceLoginProfile(
        UUID userId,
        String faceToken,
        String faceId,
        String profilePhotoUrl,
        String profilePhotoContentType,
        boolean enabled,
        Instant enrolledAt,
        Instant updatedAt
) {
}
