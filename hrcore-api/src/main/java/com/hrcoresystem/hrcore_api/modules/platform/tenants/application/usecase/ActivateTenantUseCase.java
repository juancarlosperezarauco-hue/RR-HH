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
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.service.TenantNotificationDispatcher;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivateTenantUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformTenantMapper platformTenantMapper;
    private final AuditTrailService auditTrailService;
    private final TenantNotificationDispatcher notifications;

    public ActivateTenantUseCase(PlatformTenantRepository repository, PlatformTenantMapper mapper, AuditTrailService audit) {
        this(repository, mapper, audit, null);
    }

    @Autowired
    public ActivateTenantUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformTenantMapper platformTenantMapper,
            AuditTrailService auditTrailService, TenantNotificationDispatcher notifications
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformTenantMapper = platformTenantMapper;
        this.auditTrailService = auditTrailService;
        this.notifications = notifications;
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
                tenant.stripeCustomerId(),
                true,
                tenant.legalName(),
                tenant.taxIdentifier(),
                tenant.contactEmail(),
                tenant.contactPhone(),
                tenant.ownerName(),
                tenant.ownerEmail(),
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
        if (notifications != null) notifications.toOwner(saved, NotificationType.ACCOUNT_ACTIVATED, "Tenant reactivado", "El acceso al tenant ha sido reactivado.", NotificationCategory.SECURITY, NotificationPriority.HIGH, null);

        return platformTenantMapper.toResponse(saved);
    }
}
