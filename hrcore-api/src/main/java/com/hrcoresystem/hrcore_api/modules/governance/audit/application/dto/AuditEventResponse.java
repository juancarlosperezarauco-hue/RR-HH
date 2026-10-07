package com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String actorSubject,
        UUID actorId,
        String actorEmail,
        String tenantSlug,
        String eventType,
        String resourceType,
        String resourceId,
        String eventDetails,
        String ipAddress,
        String userAgent,
        String requestId,
        String correlationId,
        String source,
        String outcome,
        String beforeState,
        String afterState,
        Instant createdAt
) {
}
