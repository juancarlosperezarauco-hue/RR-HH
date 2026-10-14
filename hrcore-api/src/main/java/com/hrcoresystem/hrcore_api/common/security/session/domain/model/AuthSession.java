package com.hrcoresystem.hrcore_api.common.security.session.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        UUID id,
        UUID userId,
        UUID tokenFamilyId,
        String refreshTokenHash,
        long refreshGeneration,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant lastRotatedAt,
        Instant revokedAt,
        String userAgent,
        String ipAddress
) {
}
