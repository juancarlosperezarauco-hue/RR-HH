package com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupRequest;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.dto.PublicPaidSignupResponse;
import com.hrcoresystem.hrcore_api.modules.platform.onboarding.application.service.TenantOwnerAdminProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;
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
public class PublicPaidSignupUseCase {

    private final CreateTenantUseCase createTenantUseCase;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final TenantOwnerAdminProvisioningService tenantOwnerAdminProvisioningService;
    private final AuditTrailService auditTrailService;

    public PublicPaidSignupUseCase(
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
    public PublicPaidSignupResponse execute(PublicPaidSignupRequest request) {
        PlatformPlan selectedPlan = platformPlanRepository.findByCode(request.planCode().trim().toUpperCase())
                .orElseThrow(() -> new BillingException("Billing plan not found: " + request.planCode()));

        BillingInterval billingInterval = parseBillingInterval(request.billingInterval());

        validateSelectedPlan(selectedPlan);

        PlatformTenantResponse createdTenantResponse = createTenantUseCase.execute(
                new CreateTenantRequest(
                        request.companyName().trim(),
                        request.tenantSlug().trim(),
                        selectedPlan.code()
                )
        );

        PlatformTenant createdTenant = platformTenantRepository.findById(createdTenantResponse.id())
                .orElseThrow();

        tenantOwnerAdminProvisioningService.provisionOwnerAdminWithoutVerification(
                createdTenant.schemaName(),
                createdTenant.slug(),
                request.adminEmail().trim().toLowerCase(),
                request.password(),
                normalizeName(request.firstName()),
                normalizeName(request.lastName())
        );

        PlatformSubscription currentSubscription = platformSubscriptionRepository.findCurrentByTenantId(createdTenant.id())
                .orElseThrow();

        PlatformPlan currentPlan = platformPlanRepository.findById(currentSubscription.planId())
                .orElseThrow();

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.PUBLIC_SIGNUP_COMPLETED,
                "TENANT",
                createdTenant.id().toString(),
                PlatformAuditPayloads.details(
                        "tenantSlug", createdTenant.slug(),
                        "adminEmail", request.adminEmail().trim().toLowerCase(),
                        "initialPlanCode", currentPlan.code(),
                        "selectedPlanCode", selectedPlan.code(),
                        "billingInterval", billingInterval.name(),
                        "paymentRequired", "false"
                ),
                null,
                PlatformAuditPayloads.tenantState(createdTenant)
        );

        return new PublicPaidSignupResponse(
                createdTenant.id(),
                createdTenant.slug(),
                createdTenant.name(),
                request.adminEmail().trim().toLowerCase(),
                "OWNER_ADMIN",
                currentPlan.code(),
                selectedPlan.code(),
                billingInterval.name(),
                null,
                null,
                "NOT_REQUIRED",
                currentSubscription.expiresAt(),
                "Organization and owner account created with the selected plan. Payment is not required for this Sprint."
        );
    }

    private void validateSelectedPlan(PlatformPlan plan) {
        if (!plan.active()) {
            throw new BillingException("Selected plan is not active");
        }

        if (!plan.publicVisible()) {
            throw new BillingException("Selected plan is not available for public signup");
        }

        if (!"PAID".equalsIgnoreCase(plan.planType())) {
            throw new BillingException("Only paid plans can be used for paid signup");
        }
    }

    private BillingInterval parseBillingInterval(String value) {
        try {
            return BillingInterval.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BillingException("Invalid billing interval. Allowed values: MONTHLY, YEARLY");
        }
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim();
    }
}
