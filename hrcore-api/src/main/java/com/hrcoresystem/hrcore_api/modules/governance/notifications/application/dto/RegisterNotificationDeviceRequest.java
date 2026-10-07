package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPlatform;

public record RegisterNotificationDeviceRequest(
        String deviceId,
        String fcmToken,
        NotificationPlatform platform,
        String deviceName,
        String appVersion,
        String osVersion
) {
}
