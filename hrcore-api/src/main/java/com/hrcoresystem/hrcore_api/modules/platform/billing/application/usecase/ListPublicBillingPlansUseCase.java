package com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.PublicBillingPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ListPublicBillingPlansUseCase {

    private final PlatformPlanRepository platformPlanRepository;

    public ListPublicBillingPlansUseCase(PlatformPlanRepository platformPlanRepository) {
        this.platformPlanRepository = platformPlanRepository;
    }

    public List<PublicBillingPlanResponse> execute() {
        return platformPlanRepository.findActivePublicPlans()
                .stream()
                .sorted(Comparator.comparingInt(PlatformPlan::sortOrder).thenComparing(PlatformPlan::code))
                .map(this::toResponse)
                .toList();
    }

    private PublicBillingPlanResponse toResponse(PlatformPlan plan) {
        boolean contactSales = isEnterprise(plan);
        boolean availableForCheckout = !contactSales;

        return new PublicBillingPlanResponse(
                plan.id(),
                plan.code(),
                plan.name(),
                plan.description(),
                plan.planType(),
                plan.maxUsers(),
                plan.maxRoles(),
                plan.trialDays(),
                plan.monthlyAmount(),
                plan.yearlyAmount(),
                plan.currency(),
                plan.publicVisible(),
                plan.sortOrder(),
                plan.active(),
                false,
                availableForCheckout,
                contactSales
        );
    }

    private boolean isEnterprise(PlatformPlan plan) {
        return "ENTERPRISE".equalsIgnoreCase(plan.code())
                || "ENTERPRISE".equalsIgnoreCase(plan.planType());
    }
}
