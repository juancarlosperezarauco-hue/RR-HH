package com.hrcoresystem.hrcore_api.modules.governance.audit.application.service;

import com.hrcoresystem.hrcore_api.common.audit.AuditContextFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.PlatformAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.PlatformAuditEventRepository;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.TenantAuditEventRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditTrailService {

    private final PlatformAuditEventRepository platformAuditEventRepository;
    private final TenantAuditEventRepository tenantAuditEventRepository;
    private final AuditContextFacade auditContextFacade;
    private final ObjectMapper objectMapper;

    public AuditTrailService(
            PlatformAuditEventRepository platformAuditEventRepository,
            TenantAuditEventRepository tenantAuditEventRepository,
            AuditContextFacade auditContextFacade,
            ObjectMapper objectMapper
    ) {
        this.platformAuditEventRepository = platformAuditEventRepository;
        this.tenantAuditEventRepository = tenantAuditEventRepository;
        this.auditContextFacade = auditContextFacade;
        this.objectMapper = objectMapper;
    }

    public void recordPlatformEvent(
            String eventType,
            String resourceType,
            String resourceId,
            Object details
    ) {
        recordPlatformEvent(eventType, resourceType, resourceId, details, null, null);
    }

    public void recordPlatformEvent(
            String eventType,
            String resourceType,
            String resourceId,
            Object details,
            Object beforeState,
            Object afterState
    ) {
        AuditContextFacade.AuditContext context = auditContextFacade.resolve();
        platformAuditEventRepository.save(
                new PlatformAuditEvent(
                        null,
                        context.actorSubject(),
                        context.actorId(),
                        context.actorEmail(),
                        context.tenantSlug(),
                        eventType,
                        resourceType,
                        resourceId,
                        serializeDetails(details),
                        context.ipAddress(),
                        context.userAgent(),
                        context.requestId(),
                        context.correlationId(),
                        context.source(),
                        context.outcome(),
                        serializeValue(beforeState),
                        serializeValue(afterState),
                        null
                )
        );
    }

    public void recordTenantEvent(
            String eventType,
            String resourceType,
            String resourceId,
            Object details
    ) {
        recordTenantEvent(eventType, resourceType, resourceId, details, null, null);
    }

    public void recordTenantEvent(
            String eventType,
            String resourceType,
            String resourceId,
            Object details,
            Object beforeState,
            Object afterState
    ) {
        AuditContextFacade.AuditContext context = auditContextFacade.resolve();
        tenantAuditEventRepository.save(
                new TenantAuditEvent(
                        null,
                        context.actorSubject(),
                        context.actorId(),
                        context.actorEmail(),
                        context.tenantSlug(),
                        eventType,
                        resourceType,
                        resourceId,
                        serializeDetails(details),
                        context.ipAddress(),
                        context.userAgent(),
                        context.requestId(),
                        context.correlationId(),
                        context.source(),
                        context.outcome(),
                        serializeValue(beforeState),
                        serializeValue(afterState),
                        null
                )
        );
    }

    private JsonNode serializeValue(Object value) {
        if (value == null) {
            return null;
        }

        return serializeValueAsJsonNode(value);
    }

    private JsonNode serializeValueAsJsonNode(Object value) {
        JsonNode node = objectMapper.valueToTree(value);
        if (node == null) {
            throw new IllegalStateException("Unable to serialize audit state");
        }

        return node;
    }

    private String serializeDetails(Object details) {
        if (details == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return String.valueOf(details);
        }
    }
}
