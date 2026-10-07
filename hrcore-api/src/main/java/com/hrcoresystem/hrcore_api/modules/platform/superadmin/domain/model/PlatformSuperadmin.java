package com.hrcoresystem.hrcore_api.modules.platform.superadmin.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PlatformSuperadmin(
        UUID id,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}