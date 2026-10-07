package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;

public record UpsertNotificationPreferenceRequest(
        NotificationCategory category,
        boolean pushEnabled,
        boolean inAppEnabled,
        boolean emailEnabled,
        boolean smsEnabled
) {
}
