package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AiConversationResponse(
        UUID id,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastMessageAt,
        int version) {
}
