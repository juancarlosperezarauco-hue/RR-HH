package com.hrcoresystem.hrcore_api.modules.platform.auth.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthenticatedPlatformSuperadminResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active,
        String tenantSlug,
        String scope,
        List<String> roles,
        List<String> permissions,
        long securityVersion,
        long catalogVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
