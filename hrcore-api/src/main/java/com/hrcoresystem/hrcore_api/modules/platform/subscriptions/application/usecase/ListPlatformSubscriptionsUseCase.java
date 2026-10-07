package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.PlatformSubscriptionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.mapper.PlatformSubscriptionMapper;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListPlatformSubscriptionsUseCase {

    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionMapper platformSubscriptionMapper;
    private final PlatformSubscriptionLifecycleService lifecycleService;

    public ListPlatformSubscriptionsUseCase(
            PlatformSubscriptionRepository platformSubscriptionRepository,
            PlatformTenantRepository platformTenantRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSubscriptionMapper platformSubscriptionMapper,
            PlatformSubscriptionLifecycleService lifecycleService
    ) {
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.platformTenantRepository = platformTenantRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.platformSubscriptionMapper = platformSubscriptionMapper;
        this.lifecycleService = lifecycleService;
    }

    public List<PlatformSubscriptionResponse> execute() {
        lifecycleService.refreshExpiredSubscriptions();

        return platformSubscriptionRepository.findAll()
                .stream()
                .map(subscription -> platformSubscriptionMapper.toResponse(
                        subscription,
                        platformTenantRepository.findById(subscription.tenantId()).orElseThrow(),
                        platformPlanRepository.findById(subscription.planId()).orElseThrow()
                ))
                .toList();
    }
}
