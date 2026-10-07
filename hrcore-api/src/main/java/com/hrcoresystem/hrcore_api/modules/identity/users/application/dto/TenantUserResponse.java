package com.hrcoresystem.hrcore_api.modules.identity.users.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}