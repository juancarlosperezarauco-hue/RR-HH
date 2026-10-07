package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID id,
        UUID userId,
        String category,
        boolean pushEnabled,
        boolean inAppEnabled,
        boolean emailEnabled,
        boolean smsEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}
