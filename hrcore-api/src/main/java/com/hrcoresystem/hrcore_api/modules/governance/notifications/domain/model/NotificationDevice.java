package com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationDevice(
        UUID id,
        UUID userId,
        String deviceId,
        String fcmToken,
        NotificationPlatform platform,
        String deviceName,
        String appVersion,
        String osVersion,
        NotificationDeviceStatus status,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
) {
}
