package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationDelivery(
        UUID id,
        UUID notificationId,
        UUID deviceId,
        NotificationChannel channel,
        String provider,
        NotificationDeliveryStatus status,
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
