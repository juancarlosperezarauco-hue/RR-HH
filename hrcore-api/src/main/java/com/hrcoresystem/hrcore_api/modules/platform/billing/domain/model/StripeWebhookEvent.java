package com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record StripeWebhookEvent(
        UUID id,
        String stripeEventId,
        String eventType,
        boolean processed,
        Instant processedAt,
        int processingAttempts,
        String lastError,
        JsonNode payload,
        Instant receivedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
