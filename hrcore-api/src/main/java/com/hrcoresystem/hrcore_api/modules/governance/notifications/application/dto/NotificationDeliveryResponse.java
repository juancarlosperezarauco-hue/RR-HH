package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryResponse(
        UUID id,
        UUID notificationId,
        UUID deviceId,
        String channel,
        String provider,
        String status,
        String providerMessageId,
        String errorCode,
        String errorMessage,
        Instant attemptedAt,
        Instant sentAt,
        Instant failedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
