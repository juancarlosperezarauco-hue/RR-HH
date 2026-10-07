package com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.usecase;

import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.dto.TenantSummaryResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception.PlatformSubscriptionNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class GetTenantSummaryUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PlatformSubscriptionLifecycleService lifecycleService;

    public GetTenantSummaryUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformPlanRepository platformPlanRepository,
            TenantUserRepository tenantUserRepository,
            PlatformSubscriptionLifecycleService lifecycleService
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.lifecycleService = lifecycleService;
    }

    public TenantSummaryResponse execute(String tenantSlug) {
        lifecycleService.refreshExpiredSubscriptions();

        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException(
                        "Tenant not found for slug: " + tenantSlug
                ));

        PlatformSubscription subscription = platformSubscriptionRepository.findCurrentByTenantId(tenant.id())
                .orElseThrow(() -> new PlatformSubscriptionNotFoundException(
                        "Current subscription not found for tenant: " + tenantSlug
                ));

        PlatformPlan plan = platformPlanRepository.findById(subscription.planId())
                .orElseThrow(() -> new PlatformSubscriptionNotFoundException(
                        "Plan not found for current subscription"
                ));

        long totalUsers = tenantUserRepository.countActiveAndPendingUsers();

        long trialDaysLeft = 0;
        if (subscription.trial() && subscription.expiresAt() != null) {
            long days = ChronoUnit.DAYS.between(Instant.now(), subscription.expiresAt());
            trialDaysLeft = Math.max(0, days);
        }

        return new TenantSummaryResponse(
                totalUsers,
                plan.maxUsers(),
                plan.code(),
                trialDaysLeft
        );
    }
}
