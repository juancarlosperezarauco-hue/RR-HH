package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CheckoutSessionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CreateCheckoutSessionRequest;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.exception.BillingException;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.BillingInterval;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTenantCheckoutSessionUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionProvisioningService subscriptionProvisioningService;

    public CreateTenantCheckoutSessionUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSubscriptionProvisioningService subscriptionProvisioningService
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.subscriptionProvisioningService = subscriptionProvisioningService;
    }

    @Transactional
    public CheckoutSessionResponse execute(CreateCheckoutSessionRequest request) {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();

        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        PlatformPlan plan = platformPlanRepository.findByCode(request.planCode().trim().toUpperCase())
                .orElseThrow(() -> new BillingException("Billing plan not found: " + request.planCode()));

        BillingInterval billingInterval = parseBillingInterval(request.billingInterval());

        validatePlanForSelection(plan);

        PlatformSubscription subscription = subscriptionProvisioningService.assignCurrentSubscription(
                tenant.id(),
                plan.code(),
                null,
                true
        );

        return new CheckoutSessionResponse(
                subscription.id(),
                null,
                null,
                "COMPLETED",
                plan.code(),
                billingInterval.name(),
                subscription.expiresAt()
        );
    }

    private void validatePlanForSelection(PlatformPlan plan) {
        if (!plan.active()) {
            throw new BillingException("Selected plan is not active");
        }

        if (!plan.publicVisible()) {
            throw new BillingException("Selected plan is not available");
        }

        if ("DEMO".equalsIgnoreCase(plan.planType())) {
            throw new BillingException("Demo plan cannot be selected from subscription settings");
        }

        if ("ENTERPRISE".equalsIgnoreCase(plan.planType())) {
            throw new BillingException("Enterprise plan requires commercial contact");
        }
    }

    private BillingInterval parseBillingInterval(String value) {
        try {
            return BillingInterval.valueOf(value.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BillingException("Invalid billing interval. Allowed values: MONTHLY, YEARLY");
        }
    }

}
