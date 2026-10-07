package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID userId,
        NotificationType type,
        NotificationCategory category,
        NotificationPriority priority,
        String title,
        String body,
        JsonNode data,
        String imageUrl,
        String actionUrl,
        Instant readAt,
        Instant openedAt,
        Instant archivedAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
