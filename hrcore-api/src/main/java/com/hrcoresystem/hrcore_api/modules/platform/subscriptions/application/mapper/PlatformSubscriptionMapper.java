package com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.mapper;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.dto.PlatformSubscriptionResponse;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.model.PlatformSubscription;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class PlatformSubscriptionMapper {

    public PlatformSubscriptionResponse toResponse(
            PlatformSubscription subscription,
            PlatformTenant tenant,
            PlatformPlan plan
    ) {
        Long remainingDays = null;

        Instant endDate = subscription.currentPeriodEnd() != null
                ? subscription.currentPeriodEnd()
                : subscription.expiresAt();

        if (endDate != null) {
            long days = Duration.between(Instant.now(), endDate).toDays();
            remainingDays = Math.max(days, 0);
        }

        return new PlatformSubscriptionResponse(
                subscription.id(),
                subscription.tenantId(),
                tenant.name(),
                tenant.slug(),
                subscription.planId(),
                plan.code(),
                plan.name(),
                plan.planType(),
                plan.maxUsers(),
                plan.maxRoles(),
                subscription.status().name(),
                subscription.trial(),
                subscription.currentSubscription(),
                subscription.stripeSubscriptionId(),
                subscription.stripePriceId(),
                subscription.billingInterval() == null ? null : subscription.billingInterval().name(),
                subscription.startedAt(),
                subscription.expiresAt(),
                subscription.currentPeriodStart(),
                subscription.currentPeriodEnd(),
                subscription.cancelAtPeriodEnd(),
                subscription.cancelledAt(),
                remainingDays,
                subscription.createdAt(),
                subscription.updatedAt()
        );
    }
}
