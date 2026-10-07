package com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.PlatformAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class AuditEventMapper {

    public AuditEventResponse toResponse(PlatformAuditEvent event) {
        return new AuditEventResponse(
                event.id(),
                event.actorSubject(),
                event.actorId(),
                event.actorEmail(),
                event.tenantSlug(),
                event.eventType(),
                event.resourceType(),
                event.resourceId(),
                event.eventDetails(),
                toIpAddress(event.ipAddress()),
                event.userAgent(),
                event.requestId(),
                event.correlationId(),
                event.source(),
                event.outcome(),
                toJson(event.beforeState()),
                toJson(event.afterState()),
                event.createdAt()
        );
    }

    public AuditEventResponse toResponse(TenantAuditEvent event) {
        return new AuditEventResponse(
                event.id(),
                event.actorSubject(),
                event.actorId(),
                event.actorEmail(),
                event.tenantSlug(),
                event.eventType(),
                event.resourceType(),
                event.resourceId(),
                event.eventDetails(),
                toIpAddress(event.ipAddress()),
                event.userAgent(),
                event.requestId(),
                event.correlationId(),
                event.source(),
                event.outcome(),
                toJson(event.beforeState()),
                toJson(event.afterState()),
                event.createdAt()
        );
    }

    private String toJson(JsonNode node) {
        return node == null ? null : node.toString();
    }

    private String toIpAddress(java.net.InetAddress address) {
        return address == null ? null : address.getHostAddress();
    }
}
