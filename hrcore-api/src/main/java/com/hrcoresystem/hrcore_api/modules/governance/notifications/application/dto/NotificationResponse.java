package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String type,
        String category,
        String priority,
        String status,
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
