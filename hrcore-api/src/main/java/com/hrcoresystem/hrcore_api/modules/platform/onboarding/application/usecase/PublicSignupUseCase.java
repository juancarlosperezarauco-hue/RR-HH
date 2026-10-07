package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.CreateTenantRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase.CreateTenantUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicSignupUseCase {

    private final CreateTenantUseCase createTenantUseCase;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService;
    private final AuditTrailService auditTrailService;

    public PublicSignupUseCase(
            CreateTenantUseCase createTenantUseCase,
            PlatformTenantRepository platformTenantRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformPlanRepository platformPlanRepository,
            TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService,
            AuditTrailService auditTrailService
    ) {
        this.createTenantUseCase = createTenantUseCase;
        this.platformTenantRepository = platformTenantRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.tenantOwnerAdminProvisioningService = tenantOwnerAdminProvisioningService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PublicSignupResponse execute(PublicSignupRequest request) {
        PlatformTenantResponse createdTenantResponse = createTenantUseCase.execute(
                new CreateTenantRequest(
                        request.companyName().trim(),
                        request.tenantSlug().trim(),
                        "DEMO"
                )
        );

        PlatformTenant createdTenant = platformTenantRepository.findById(createdTenantResponse.id())
                .orElseThrow();

        tenantOwnerAdminProvisioningService.provisionOwnerAdmin(
                createdTenant.schemaName(),
                createdTenant.slug(),
                request.adminEmail(),
                request.password(),
                request.firstName(),
                request.lastName()
        );

        PlatformSubscription currentSubscription = platformSubscriptionRepository.findCurrentByTenantId(createdTenant.id())
                .orElseThrow();

        PlatformPlan currentPlan = platformPlanRepository.findById(currentSubscription.planId()).orElseThrow();

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.PUBLIC_SIGNUP_COMPLETED,
                "TENANT",
                createdTenant.id().toString(),
                PlatformAuditPayloads.details(
                        "tenantSlug", createdTenant.slug(),
                        "adminEmail", request.adminEmail().trim().toLowerCase(),
                        "planCode", currentPlan.code(),
                        "subscriptionStatus", currentSubscription.status().name()
                ),
                null,
                PlatformAuditPayloads.tenantState(createdTenant)
        );

        return new PublicSignupResponse(
                createdTenant.id(),
                createdTenant.slug(),
                createdTenant.name(),
                request.adminEmail().trim().toLowerCase(),
                "OWNER_ADMIN",
                currentPlan.code(),
                currentSubscription.status().name(),
                currentSubscription.expiresAt(),
                "Revisa tu correo y activa la cuenta antes de iniciar sesión (X-Tenant-Slug = "
                        + createdTenant.slug() + ")."
        );
    }
}
