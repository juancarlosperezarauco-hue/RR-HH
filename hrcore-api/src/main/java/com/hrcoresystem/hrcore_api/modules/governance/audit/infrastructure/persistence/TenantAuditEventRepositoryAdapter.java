package com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.TenantAuditFilterCriteria;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.TenantAuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantAuditEventRepositoryAdapter implements TenantAuditEventRepository {

    private static final Sort AUDIT_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final TenantAuditEventJpaRepository jpaRepository;

    public TenantAuditEventRepositoryAdapter(TenantAuditEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TenantAuditEvent save(TenantAuditEvent event) {
        TenantAuditEventEntity entity = toEntity(event);
        TenantAuditEventEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TenantAuditEvent> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TenantAuditEvent> findAll() {
        return jpaRepository.findAll(AUDIT_SORT)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Page<TenantAuditEvent> findAll(TenantAuditFilterCriteria criteria, Pageable pageable) {
        Pageable effectivePageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), AUDIT_SORT);
        }

        return jpaRepository.findAll(TenantAuditEventSpecifications.fromCriteria(criteria), effectivePageable)
                .map(this::toDomain);
    }

    @Override
    public List<TenantAuditEvent> findRecent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jpaRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    private TenantAuditEventEntity toEntity(TenantAuditEvent event) {
        TenantAuditEventEntity entity = new TenantAuditEventEntity();
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

    private TenantAuditEvent toDomain(TenantAuditEventEntity entity) {
        return new TenantAuditEvent(
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
