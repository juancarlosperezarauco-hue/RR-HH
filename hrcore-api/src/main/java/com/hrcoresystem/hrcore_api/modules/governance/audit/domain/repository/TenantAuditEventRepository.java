package com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.TenantAuditFilterCriteria;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantAuditEventRepository {

    TenantAuditEvent save(TenantAuditEvent event);

    Optional<TenantAuditEvent> findById(UUID id);

    List<TenantAuditEvent> findAll();

    Page<TenantAuditEvent> findAll(TenantAuditFilterCriteria criteria, Pageable pageable);

    List<TenantAuditEvent> findRecent(int limit);

    long count();
}
