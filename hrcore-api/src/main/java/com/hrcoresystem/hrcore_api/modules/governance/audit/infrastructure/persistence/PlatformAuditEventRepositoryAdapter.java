package com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.PlatformAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.PlatformAuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PlatformAuditEventRepositoryAdapter implements PlatformAuditEventRepository {

    private final PlatformAuditEventJpaRepository jpaRepository;

    public PlatformAuditEventRepositoryAdapter(PlatformAuditEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlatformAuditEvent save(PlatformAuditEvent event) {
        PlatformAuditEventEntity entity = toEntity(event);
        PlatformAuditEventEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<PlatformAuditEvent> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PlatformAuditEvent> findRecent(int limit) {
        return jpaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    private PlatformAuditEventEntity toEntity(PlatformAuditEvent event) {
        PlatformAuditEventEntity entity = new PlatformAuditEventEntity();
        entity.setId(event.id());
        entity.setActorSubject(event.actorSubject());
        entity.setActorId(event.actorId());
        entity.setActorEmail(event.actorEmail());
        entity.setTenantSlug(event.tenantSlug());
        entity.setEventType(event.eventType());
        entity.setResourceType(event.resourceType());
        entity.setResourceId(event.resourceId());
        entity.setEventDetails(event.eventDetails());
        entity.setIpAddress(event.ipAddress());
        entity.setUserAgent(event.userAgent());
        entity.setRequestId(event.requestId());
        entity.setCorrelationId(event.correlationId());
        entity.setSource(event.source());
        entity.setOutcome(event.outcome());
        entity.setBeforeState(event.beforeState());
        entity.setAfterState(event.afterState());
        return entity;
    }

    private PlatformAuditEvent toDomain(PlatformAuditEventEntity entity) {
        return new PlatformAuditEvent(
                entity.getId(),
                entity.getActorSubject(),
                entity.getActorId(),
                entity.getActorEmail(),
                entity.getTenantSlug(),
                entity.getEventType(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getEventDetails(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getRequestId(),
                entity.getCorrelationId(),
                entity.getSource(),
                entity.getOutcome(),
                entity.getBeforeState(),
                entity.getAfterState(),
                entity.getCreatedAt()
        );
    }
}
