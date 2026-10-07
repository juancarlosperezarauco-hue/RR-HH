package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.exception.PlatformPlanNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PlatformSubscriptionProvisioningService {

    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final AuditTrailService auditTrailService;

    public PlatformSubscriptionProvisioningService(
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformTenantRepository platformTenantRepository,
            PlatformPlanRepository platformPlanRepository,
            AuditTrailService auditTrailService
    ) {
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformTenantRepository = platformTenantRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PlatformSubscription assignCurrentSubscription(
            UUID tenantId,
            String requestedPlanCode,
            Integer overrideTrialDays,
            boolean recordAudit
    ) {
        PlatformTenant tenant = platformTenantRepository.findById(tenantId)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with id: " + tenantId));

        String normalizedPlanCode = requestedPlanCode == null || requestedPlanCode.isBlank()
                ? "DEMO"
                : requestedPlanCode.trim().toUpperCase();

        PlatformPlan plan = platformPlanRepository.findByCode(normalizedPlanCode)
                .filter(PlatformPlan::active)
                .orElseThrow(() -> new PlatformPlanNotFoundException(
                        "Active platform plan not found for code: " + normalizedPlanCode
                ));

        Instant startedAt = Instant.now();
        PlatformSubscriptionStatus status;
        boolean isTrial;
        Instant expiresAt = null;
        PlatformSubscription previousCurrentSubscription = platformSubscriptionRepository.findCurrentByTenantId(tenant.id()).orElse(null);

        if ("DEMO".equalsIgnoreCase(plan.planType())) {
            int effectiveTrialDays = overrideTrialDays != null
                    ? overrideTrialDays
                    : (plan.trialDays() != null ? plan.trialDays() : 10);

            if (effectiveTrialDays <= 0) {
                throw new BusinessException("Trial days must be greater than zero");
            }

            status = PlatformSubscriptionStatus.TRIAL;
            isTrial = true;
            expiresAt = startedAt.plus(effectiveTrialDays, ChronoUnit.DAYS);
        } else {
            status = PlatformSubscriptionStatus.ACTIVE;
            isTrial = false;
        }

        platformSubscriptionRepository.clearCurrentSubscriptions(tenant.id());

        PlatformSubscription subscriptionToCreate = new PlatformSubscription(
                null,
                tenant.id(),
                plan.id(),
                status,
                isTrial,
                true,
                startedAt,
                expiresAt,
                null,
                null
        );

        PlatformSubscription created = platformSubscriptionRepository.save(subscriptionToCreate);

        PlatformTenant updatedTenant = new PlatformTenant(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.schemaName(),
                tenant.status(),
                plan.id(),
                tenant.active(),
                tenant.createdAt(),
                tenant.updatedAt()
        );

        platformTenantRepository.save(updatedTenant);

        if (recordAudit) {
            auditTrailService.recordPlatformEvent(
                    AuditEventTypes.SUBSCRIPTION_ASSIGNED,
                    "TENANT_SUBSCRIPTION",
                    created.id().toString(),
                    PlatformAuditPayloads.details(
                            "tenantId", tenant.id().toString(),
                            "tenantSlug", tenant.slug(),
                            "planCode", plan.code(),
                            "planType", plan.planType(),
                            "status", created.status().name(),
                            "trial", created.trial(),
                            "currentSubscription", created.currentSubscription(),
                            "expiresAt", created.expiresAt()
                    ),
                    previousCurrentSubscription,
                    created
            );
        }

        return created;
    }
}
