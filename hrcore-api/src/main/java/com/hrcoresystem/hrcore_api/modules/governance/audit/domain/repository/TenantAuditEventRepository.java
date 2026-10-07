package com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;

import java.util.List;

public interface TenantAuditEventRepository {

    TenantAuditEvent save(TenantAuditEvent event);

    List<TenantAuditEvent> findAll();

    List<TenantAuditEvent> findRecent(int limit);

    long count();
}
