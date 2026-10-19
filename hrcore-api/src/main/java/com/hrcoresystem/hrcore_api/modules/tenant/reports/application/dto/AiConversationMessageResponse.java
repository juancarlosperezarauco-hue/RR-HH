package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AiConversationMessageResponse(
        UUID id,
        String role,
        String content,
        String messageType,
        String state,
        Instant createdAt,
        Instant completedAt,
        UUID reportRunId) {
    public AiConversationMessageResponse(UUID id, String role, String content, String messageType,
                                         String state, Instant createdAt, Instant completedAt) {
        this(id, role, content, messageType, state, createdAt, completedAt, null);
    }
}
