package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreference(
        UUID id,
        UUID userId,
        NotificationCategory category,
        boolean pushEnabled,
        boolean inAppEnabled,
        boolean emailEnabled,
        boolean smsEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}
