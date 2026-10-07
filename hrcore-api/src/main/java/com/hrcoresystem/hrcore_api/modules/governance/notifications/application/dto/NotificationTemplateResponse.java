package com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String type,
        String channel,
        String titleTemplate,
        String bodyTemplate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
