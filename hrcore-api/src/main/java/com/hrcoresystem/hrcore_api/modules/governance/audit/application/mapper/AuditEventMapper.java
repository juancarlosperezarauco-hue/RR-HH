package com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.PlatformAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class AuditEventMapper {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final TenantUserRepository tenantUserRepository;

    @Autowired
    public AuditEventMapper(@Autowired(required = false) TenantUserRepository tenantUserRepository) {
        this.tenantUserRepository = tenantUserRepository;
    }

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
        String resolvedActor = resolveActorName(event.actorSubject(), event.actorId(), event.actorEmail());
        return new AuditEventResponse(
                event.id(),
                resolvedActor,
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

    private String resolveActorName(String actorSubject, UUID actorId, String actorEmail) {
        String actor = actorSubject != null ? actorSubject.trim() : null;
        boolean needsResolution = actor == null || "SYSTEM".equalsIgnoreCase(actor) || UUID_PATTERN.matcher(actor).matches();

        if (needsResolution && tenantUserRepository != null) {
            if (actorId != null) {
                var userOpt = tenantUserRepository.findById(actorId);
                if (userOpt.isPresent()) {
                    var u = userOpt.get();
                    String full = ((u.firstName() != null ? u.firstName() : "") + " " + (u.lastName() != null ? u.lastName() : "")).trim();
                    if (!full.isBlank()) return full;
                }
            }
            if (actorEmail != null && !actorEmail.isBlank()) {
                var userOpt = tenantUserRepository.findByEmail(actorEmail.trim());
                if (userOpt.isPresent()) {
                    var u = userOpt.get();
                    String full = ((u.firstName() != null ? u.firstName() : "") + " " + (u.lastName() != null ? u.lastName() : "")).trim();
                    if (!full.isBlank()) return full;
                }
            }
        }

        if (actor == null || "SYSTEM".equalsIgnoreCase(actor)) {
            return "Sistema";
        }

        return actor;
    }

    private String toJson(JsonNode node) {
        return node == null ? null : node.toString();
    }

    private String toIpAddress(java.net.InetAddress address) {
        return address == null ? null : address.getHostAddress();
    }
}
