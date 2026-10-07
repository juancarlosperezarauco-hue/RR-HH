package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeviceResponse(
        UUID id,
        UUID userId,
        String deviceId,
        String platform,
        String deviceName,
        String appVersion,
        String osVersion,
        String status,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
) {
}
