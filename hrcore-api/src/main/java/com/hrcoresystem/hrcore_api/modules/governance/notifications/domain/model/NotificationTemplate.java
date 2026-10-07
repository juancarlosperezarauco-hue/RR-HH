package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationTemplate(
        UUID id,
        NotificationType type,
        NotificationChannel channel,
        String titleTemplate,
        String bodyTemplate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
