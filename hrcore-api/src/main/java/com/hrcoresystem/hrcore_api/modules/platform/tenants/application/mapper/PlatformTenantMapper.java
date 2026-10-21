package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.domain.repository.PlatformSubscriptionRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlatformTenantMapper {

    private final PlatformPlanRepository planRepository;
    private final PlatformSubscriptionRepository subscriptionRepository;

    public PlatformTenantMapper() {
        this(null, null);
    }

    @Autowired
    public PlatformTenantMapper(
            PlatformPlanRepository planRepository,
            PlatformSubscriptionRepository subscriptionRepository
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public PlatformTenantResponse toResponse(PlatformTenant tenant) {
        if (tenant == null) {
            return null;
        }

        String planName = null;
        String planCode = null;
        String subscriptionStatus = null;

        if (tenant.id() != null && subscriptionRepository != null) {
            try {
                var currentSub = subscriptionRepository.findCurrentByTenantId(tenant.id());
                if (currentSub.isPresent()) {
                    var sub = currentSub.get();
                    subscriptionStatus = sub.status() != null ? sub.status().name() : null;
                    if (sub.planId() != null && planRepository != null) {
                        var plan = planRepository.findById(sub.planId());
                        if (plan.isPresent()) {
                            planName = plan.get().name();
                            planCode = plan.get().code();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (planName == null && tenant.planId() != null && planRepository != null) {
            try {
                var plan = planRepository.findById(tenant.planId());
                if (plan.isPresent()) {
                    planName = plan.get().name();
                    planCode = plan.get().code();
                }
            } catch (Exception ignored) {
            }
        }

        return new PlatformTenantResponse(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.schemaName(),
                tenant.status() != null ? tenant.status().name() : "INACTIVE",
                tenant.planId(),
                planName,
                planCode,
                subscriptionStatus,
                tenant.legalName(),
                tenant.taxIdentifier(),
                tenant.contactEmail(),
                tenant.contactPhone(),
                tenant.ownerName(),
                tenant.ownerEmail(),
                tenant.stripeCustomerId(),
                tenant.active(),
                tenant.createdAt(),
                tenant.updatedAt()
        );
    }
}
