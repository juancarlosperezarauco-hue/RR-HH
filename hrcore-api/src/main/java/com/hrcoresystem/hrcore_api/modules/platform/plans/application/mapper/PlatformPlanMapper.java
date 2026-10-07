package com.hrcoresystem.hrcore_api.modules.platform.plans.application.mapper;

import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.PlatformPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import org.springframework.stereotype.Component;

@Component
public class PlatformPlanMapper {

    public PlatformPlanResponse toResponse(PlatformPlan plan) {
        return new PlatformPlanResponse(
                plan.id(),
                plan.code(),
                plan.name(),
                plan.description(),
                plan.maxUsers(),
                plan.maxRoles(),
                plan.planType(),
                plan.trialDays(),
                plan.monthlyAmount(),
                plan.yearlyAmount(),
                plan.currency(),
                plan.publicVisible(),
                plan.sortOrder(),
                plan.active(),
                plan.createdAt(),
                plan.updatedAt()
        );
    }
}
