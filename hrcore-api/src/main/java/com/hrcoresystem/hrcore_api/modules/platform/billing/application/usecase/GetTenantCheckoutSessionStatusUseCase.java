package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.CheckoutSessionStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.model.SubscriptionCheckoutSession;
import com.hrcoresystem.hrcore_api.modules.platform.billing.domain.repository.SubscriptionCheckoutSessionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetTenantCheckoutSessionStatusUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final SubscriptionCheckoutSessionRepository checkoutSessionRepository;
    private final PlatformPlanRepository platformPlanRepository;

    public GetTenantCheckoutSessionStatusUseCase(
            PlatformTenantRepository platformTenantRepository,
            SubscriptionCheckoutSessionRepository checkoutSessionRepository,
            PlatformPlanRepository platformPlanRepository
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.platformPlanRepository = platformPlanRepository;
    }

    public CheckoutSessionStatusResponse execute(UUID id) {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();
        var tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        SubscriptionCheckoutSession session = checkoutSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Checkout session not found with id: " + id));

        if (!tenant.id().equals(session.tenantId())) {
            throw new IllegalArgumentException("Checkout session does not belong to the current tenant");
        }

        String planCode = platformPlanRepository.findById(session.planId()).map(plan -> plan.code()).orElse(null);

        return new CheckoutSessionStatusResponse(
                session.id(),
                session.stripeSessionId(),
                session.checkoutUrl(),
                session.status(),
                planCode,
                session.billingInterval() == null ? null : session.billingInterval().name(),
                session.expiresAt()
        );
    }
}
