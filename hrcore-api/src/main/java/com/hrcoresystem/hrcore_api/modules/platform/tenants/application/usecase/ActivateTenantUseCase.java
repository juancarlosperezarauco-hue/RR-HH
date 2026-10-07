package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivateTenantUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformTenantMapper platformTenantMapper;
    private final AuditTrailService auditTrailService;

    public ActivateTenantUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformTenantMapper platformTenantMapper,
            AuditTrailService auditTrailService
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformTenantMapper = platformTenantMapper;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PlatformTenantResponse execute(UUID id) {
        PlatformTenant tenant = platformTenantRepository.findById(id)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with id: " + id));
        PlatformTenant beforeState = tenant;

        PlatformTenant updated = new PlatformTenant(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.schemaName(),
                PlatformTenantStatus.ACTIVE,
                tenant.planId(),
                true,
                tenant.createdAt(),
                tenant.updatedAt()
        );

        PlatformTenant saved = platformTenantRepository.save(updated);

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.TENANT_ACTIVATED,
                "TENANT",
                saved.id().toString(),
                PlatformAuditPayloads.details(
                        "slug", saved.slug(),
                        "status", saved.status().name()
                ),
                PlatformAuditPayloads.tenantState(beforeState),
                PlatformAuditPayloads.tenantState(saved)
        );

        return platformTenantMapper.toResponse(saved);
    }
}
