package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.PlatformSubscriptionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.mapper.PlatformSubscriptionMapper;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.exception.PlatformSubscriptionNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentTenantSubscriptionUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionMapper platformSubscriptionMapper;
    private final PlatformSubscriptionLifecycleService lifecycleService;

    public GetCurrentTenantSubscriptionUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSubscriptionMapper platformSubscriptionMapper,
            PlatformSubscriptionLifecycleService lifecycleService
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.platformSubscriptionMapper = platformSubscriptionMapper;
        this.lifecycleService = lifecycleService;
    }

    public PlatformSubscriptionResponse execute(String tenantSlug) {
        lifecycleService.refreshExpiredSubscriptions();

        PlatformTenant tenant = platformTenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new PlatformTenantNotFoundException(
                        "Tenant not found for slug: " + tenantSlug
                ));

        PlatformSubscription subscription = platformSubscriptionRepository.findCurrentByTenantId(tenant.id())
                .orElseThrow(() -> new PlatformSubscriptionNotFoundException(
                        "Current subscription not found for tenant: " + tenantSlug
                ));

        return platformSubscriptionMapper.toResponse(
                subscription,
                tenant,
                platformPlanRepository.findById(subscription.planId()).orElseThrow()
        );
    }
}
