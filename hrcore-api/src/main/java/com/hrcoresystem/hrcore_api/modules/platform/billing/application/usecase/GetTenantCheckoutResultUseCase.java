package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingCheckoutResultResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

@Service
public class GetTenantCheckoutResultUseCase {

    private final PlatformTenantRepository tenantRepository;
    private final PlatformPlanRepository planRepository;
    private final SubscriptionCheckoutSessionRepository checkoutSessionRepository;

    public GetTenantCheckoutResultUseCase(
            PlatformTenantRepository tenantRepository,
            PlatformPlanRepository planRepository,
            SubscriptionCheckoutSessionRepository checkoutSessionRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
    }

    public BillingCheckoutResultResponse execute(String stripeSessionId) {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();

        PlatformTenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        SubscriptionCheckoutSession session = checkoutSessionRepository.findByStripeSessionId(stripeSessionId)
                .filter(found -> found.tenantId().equals(tenant.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));

        PlatformPlan plan = planRepository.findById(session.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Checkout plan not found"));

        return new BillingCheckoutResultResponse(
                session.id(),
                session.stripeSessionId(),
                session.status(),
                plan.code(),
                session.billingInterval().name(),
                session.checkoutUrl(),
                session.completedAt(),
                session.expiresAt()
        );
    }
}
