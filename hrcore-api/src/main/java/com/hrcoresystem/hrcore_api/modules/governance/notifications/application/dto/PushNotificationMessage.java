package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record PushNotificationMessage(
        UUID notificationId,
        UUID userId,
        String deviceToken,
        String title,
        String body,
        JsonNode data,
        String imageUrl,
        String actionUrl
) {
}
