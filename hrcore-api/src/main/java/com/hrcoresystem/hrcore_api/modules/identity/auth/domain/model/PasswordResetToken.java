package com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetToken(
        UUID id,
        String email,
        String token,
        Instant expiresAt,
        boolean used,
        Instant usedAt,
        Instant createdAt
) {
}