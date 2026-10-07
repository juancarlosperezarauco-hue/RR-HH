package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.AssignPlatformSubscriptionRequest;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.PlatformSubscriptionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.mapper.PlatformSubscriptionMapper;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignPlatformSubscriptionUseCase {

    private final PlatformSubscriptionProvisioningService provisioningService;
    private final PlatformSubscriptionLifecycleService lifecycleService;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionMapper platformSubscriptionMapper;

    public AssignPlatformSubscriptionUseCase(
            PlatformSubscriptionProvisioningService provisioningService,
            PlatformSubscriptionLifecycleService lifecycleService,
            PlatformTenantRepository platformTenantRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSubscriptionMapper platformSubscriptionMapper
    ) {
        this.provisioningService = provisioningService;
        this.lifecycleService = lifecycleService;
        this.platformTenantRepository = platformTenantRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.platformSubscriptionMapper = platformSubscriptionMapper;
    }

    @Transactional
    public PlatformSubscriptionResponse execute(AssignPlatformSubscriptionRequest request) {
        lifecycleService.refreshExpiredSubscriptions();

        PlatformSubscription subscription = provisioningService.assignCurrentSubscription(
                request.tenantId(),
                request.planCode(),
                request.overrideTrialDays(),
                true
        );

        PlatformTenant tenant = platformTenantRepository.findById(subscription.tenantId()).orElseThrow();
        PlatformPlan plan = platformPlanRepository.findById(subscription.planId()).orElseThrow();

        return platformSubscriptionMapper.toResponse(subscription, tenant, plan);
    }
}
