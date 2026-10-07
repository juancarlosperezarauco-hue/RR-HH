package com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository;

import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.PlatformAuditEvent;

import java.util.List;

public interface PlatformAuditEventRepository {

    PlatformAuditEvent save(PlatformAuditEvent event);

    List<PlatformAuditEvent> findAll();

    List<PlatformAuditEvent> findRecent(int limit);

    long count();
}
