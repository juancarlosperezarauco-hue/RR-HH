package com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper.AuditEventMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.TenantAuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTenantAuditEventsUseCase {

    private final TenantAuditEventRepository tenantAuditEventRepository;
    private final AuditEventMapper auditEventMapper;

    public ListTenantAuditEventsUseCase(
            TenantAuditEventRepository tenantAuditEventRepository,
            AuditEventMapper auditEventMapper
    ) {
        this.tenantAuditEventRepository = tenantAuditEventRepository;
        this.auditEventMapper = auditEventMapper;
    }

    public List<AuditEventResponse> execute() {
        return tenantAuditEventRepository.findAll()
                .stream()
                .map(auditEventMapper::toResponse)
                .toList();
    }

    public List<AuditEventResponse> execute(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);

        return tenantAuditEventRepository.findRecent(safeLimit)
                .stream()
                .map(auditEventMapper::toResponse)
                .toList();
    }
}
