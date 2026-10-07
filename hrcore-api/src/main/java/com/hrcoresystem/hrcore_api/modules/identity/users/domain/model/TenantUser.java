package com.hrcoresystem.hrcore_api.modules.identity.users.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TenantUser(
        UUID id,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        boolean active,
        TenantUserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}