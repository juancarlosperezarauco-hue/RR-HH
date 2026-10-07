package com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model;

import java.time.Instant;
import java.net.InetAddress;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;

public record PlatformAuditEvent(
        UUID id,
        String actorSubject,
        UUID actorId,
        String actorEmail,
        String tenantSlug,
        String eventType,
        String resourceType,
        String resourceId,
        String eventDetails,
        InetAddress ipAddress,
        String userAgent,
        String requestId,
        String correlationId,
        String source,
        String outcome,
        JsonNode beforeState,
        JsonNode afterState,
        Instant createdAt
) {
}
