package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model;

import java.time.Instant;

public record PasswordResetNotification(
        String email,
        String tenantSlug,
        String resetToken,
        Instant expiresAt
) {
}