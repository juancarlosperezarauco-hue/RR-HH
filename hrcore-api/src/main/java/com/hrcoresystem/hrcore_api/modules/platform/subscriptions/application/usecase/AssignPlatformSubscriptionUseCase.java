package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.AssignPlatformSubscriptionRequest;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.PlatformSubscriptionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.mapper.PlatformSubscriptionMapper;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionLifecycleService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.PlatformSubscriptionProvisioningService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.service.TenantNotificationDispatcher;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignPlatformSubscriptionUseCase {

    private final PlatformSubscriptionProvisioningService provisioningService;
    private final PlatformSubscriptionLifecycleService lifecycleService;
    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionMapper platformSubscriptionMapper;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final TenantNotificationDispatcher notifications;

    public AssignPlatformSubscriptionUseCase(
            PlatformSubscriptionProvisioningService provisioningService,
            PlatformSubscriptionLifecycleService lifecycleService,
            PlatformTenantRepository platformTenantRepository,
            PlatformPlanRepository platformPlanRepository,
            PlatformSubscriptionMapper platformSubscriptionMapper,
            PlatformSubscriptionRepository platformSubscriptionRepository,
            TenantNotificationDispatcher notifications
    ) {
        this.provisioningService = provisioningService;
        this.lifecycleService = lifecycleService;
        this.platformTenantRepository = platformTenantRepository;
        this.platformPlanRepository = platformPlanRepository;
        this.platformSubscriptionMapper = platformSubscriptionMapper;
        this.platformSubscriptionRepository = platformSubscriptionRepository;
        this.notifications = notifications;
    }

    @Transactional
    public PlatformSubscriptionResponse execute(AssignPlatformSubscriptionRequest request) {
        lifecycleService.refreshExpiredSubscriptions();

        PlatformSubscription previousSubscription = platformSubscriptionRepository
                .findCurrentByTenantId(request.tenantId())
                .orElse(null);

        PlatformSubscription subscription = provisioningService.assignCurrentSubscription(
                request.tenantId(),
                request.planCode(),
                request.overrideTrialDays(),
                true
        );

        PlatformTenant tenant = platformTenantRepository.findById(subscription.tenantId()).orElseThrow();
        PlatformPlan plan = platformPlanRepository.findById(subscription.planId()).orElseThrow();

        if (previousSubscription != null && !previousSubscription.planId().equals(subscription.planId())) {
            notifications.toOwner(
                    tenant,
                    NotificationType.SYSTEM_NOTICE,
                    "Plan actualizado",
                    "El plan de tu tenant fue cambiado a " + plan.name() + ".",
                    NotificationCategory.SYSTEM,
                    NotificationPriority.HIGH,
                    "/dashboard/settings/billing?subscriptionId=" + subscription.id()
            );
        }

        return platformSubscriptionMapper.toResponse(subscription, tenant, plan);
    }
}
