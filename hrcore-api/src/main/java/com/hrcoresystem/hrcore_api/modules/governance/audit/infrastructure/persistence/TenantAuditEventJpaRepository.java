package com.hrcoresystem.hrcore_api.modules.governance.audit.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantAuditEventJpaRepository extends JpaRepository<TenantAuditEventEntity, UUID> {

    List<TenantAuditEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}