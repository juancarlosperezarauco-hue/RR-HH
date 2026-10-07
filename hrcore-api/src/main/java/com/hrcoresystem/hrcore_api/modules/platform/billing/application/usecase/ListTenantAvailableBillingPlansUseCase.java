package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ListTenantAvailableBillingPlansUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;

    public ListTenantAvailableBillingPlansUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformPlanRepository platformPlanRepository
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformPlanRepository = platformPlanRepository;
    }

    public List<BillingPlanResponse> execute() {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();
        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with slug: " + tenantSlug));

        PlatformPlan currentPlan = platformSubscriptionRepository.findCurrentByTenantId(tenant.id())
                .flatMap(subscription -> platformPlanRepository.findById(subscription.planId()))
                .orElse(null);

        return platformPlanRepository.findActivePublicPlans()
                .stream()
                .sorted(Comparator.comparingInt(PlatformPlan::sortOrder).thenComparing(PlatformPlan::code))
                .map(plan -> new BillingPlanResponse(
                        plan.id(),
                        plan.code(),
                        plan.name(),
                        plan.description(),
                        plan.planType(),
                        plan.trialDays(),
                        plan.monthlyAmount(),
                        plan.yearlyAmount(),
                        plan.currency(),
                        plan.publicVisible(),
                        plan.sortOrder(),
                        plan.active(),
                        currentPlan != null && currentPlan.id() != null && currentPlan.id().equals(plan.id()),
                        currentPlan == null || plan.sortOrder() > currentPlan.sortOrder()
                ))
                .toList();
    }
}
