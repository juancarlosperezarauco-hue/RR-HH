package com.hrcoresystem.hrcore_api.modules.governance.audit.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto.AuditEventResponse;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.mapper.AuditEventMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.TenantAuditEvent;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.repository.TenantAuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetTenantAuditEventByIdUseCase {

    private final TenantAuditEventRepository tenantAuditEventRepository;
    private final AuditEventMapper auditEventMapper;

    public GetTenantAuditEventByIdUseCase(
            TenantAuditEventRepository tenantAuditEventRepository,
            AuditEventMapper auditEventMapper
    ) {
        this.tenantAuditEventRepository = tenantAuditEventRepository;
        this.auditEventMapper = auditEventMapper;
    }

    public AuditEventResponse execute(UUID id) {
        TenantAuditEvent event = tenantAuditEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant audit event not found with id: " + id));

        return auditEventMapper.toResponse(event);
    }
}
