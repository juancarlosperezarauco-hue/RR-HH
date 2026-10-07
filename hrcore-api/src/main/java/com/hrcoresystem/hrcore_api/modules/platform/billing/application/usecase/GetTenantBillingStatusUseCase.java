package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscriptionStatus;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class GetTenantBillingStatusUseCase {

    private final PlatformTenantRepository tenantRepository;
    private final PlatformSubscriptionRepository subscriptionRepository;
    private final PlatformPlanRepository planRepository;
    private final SecurityContextFacade securityContextFacade;

    public GetTenantBillingStatusUseCase(
            PlatformTenantRepository tenantRepository,
            PlatformSubscriptionRepository subscriptionRepository,
            PlatformPlanRepository planRepository,
            SecurityContextFacade securityContextFacade
    ) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.securityContextFacade = securityContextFacade;
    }

    public BillingStatusResponse execute() {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();

        PlatformTenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        PlatformSubscription subscription = subscriptionRepository.findCurrentByTenantId(tenant.id())
                .orElseThrow(() -> new IllegalStateException("Current subscription not found for tenant: " + tenantSlug));

        PlatformPlan plan = planRepository.findById(subscription.planId())
                .orElseThrow(() -> new IllegalStateException("Current subscription plan not found"));

        boolean canUsePlatform = subscription.status() == PlatformSubscriptionStatus.ACTIVE
                || subscription.status() == PlatformSubscriptionStatus.TRIAL;
        boolean canManageBilling = securityContextFacade.hasAuthority("billing.subscription.manage");
        boolean canOpenCustomerPortal = canManageBilling && tenant.stripeCustomerId() != null;

        Instant endDate = subscription.currentPeriodEnd() != null
                ? subscription.currentPeriodEnd()
                : subscription.expiresAt();
        Long remainingDays = null;
        if (endDate != null) {
            remainingDays = Math.max(Duration.between(Instant.now(), endDate).toDays(), 0);
        }

        return new BillingStatusResponse(
                tenant.id(),
                tenant.slug(),
                plan.code(),
                plan.name(),
                subscription.status().name(),
                subscription.billingInterval() == null ? null : subscription.billingInterval().name(),
                subscription.trial(),
                subscription.status() == PlatformSubscriptionStatus.ACTIVE,
                subscription.status() == PlatformSubscriptionStatus.PAST_DUE,
                subscription.status() == PlatformSubscriptionStatus.SUSPENDED,
                subscription.status() == PlatformSubscriptionStatus.INCOMPLETE,
                subscription.status() == PlatformSubscriptionStatus.CANCELLED,
                subscription.cancelAtPeriodEnd(),
                subscription.currentPeriodStart(),
                subscription.currentPeriodEnd(),
                subscription.expiresAt(),
                remainingDays,
                canUsePlatform,
                canManageBilling,
                canOpenCustomerPortal
        );
    }
}
