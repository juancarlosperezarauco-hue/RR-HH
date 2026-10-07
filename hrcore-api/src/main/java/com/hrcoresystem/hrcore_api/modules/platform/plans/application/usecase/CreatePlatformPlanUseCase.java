package com.hrcoresystem.hrcore_api.modules.platform.plans.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.CreatePlatformPlanRequest;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.PlatformPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.mapper.PlatformPlanMapper;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.exception.PlatformPlanAlreadyExistsException;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePlatformPlanUseCase {

    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformPlanMapper platformPlanMapper;
    private final AuditTrailService auditTrailService;

    public CreatePlatformPlanUseCase(
            PlatformPlanRepository platformPlanRepository,
            PlatformPlanMapper platformPlanMapper,
            AuditTrailService auditTrailService
    ) {
        this.platformPlanRepository = platformPlanRepository;
        this.platformPlanMapper = platformPlanMapper;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PlatformPlanResponse execute(CreatePlatformPlanRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        String normalizedPlanType = request.planType().trim().toUpperCase();

        if (platformPlanRepository.existsByCode(normalizedCode)) {
            throw new PlatformPlanAlreadyExistsException(
                    "A platform plan with code '" + normalizedCode + "' already exists"
            );
        }

        validatePlanType(normalizedPlanType, request.trialDays());

        PlatformPlan planToCreate = new PlatformPlan(
                null,
                normalizedCode,
                request.name().trim(),
                request.description() == null ? null : request.description().trim(),
                request.maxUsers(),
                request.maxRoles(),
                normalizedPlanType,
                request.trialDays(),
                request.monthlyAmount(),
                request.yearlyAmount(),
                normalizeCurrency(request.currency()),
                request.publicVisible() == null || request.publicVisible(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                true,
                null,
                null
        );

        PlatformPlan createdPlan = platformPlanRepository.save(planToCreate);

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.PLATFORM_PLAN_CREATED,
                "PLATFORM_PLAN",
                createdPlan.id().toString(),
                PlatformAuditPayloads.details(
                        "code", createdPlan.code(),
                        "name", createdPlan.name(),
                        "planType", createdPlan.planType(),
                        "trialDays", createdPlan.trialDays()
                ),
                null,
                PlatformAuditPayloads.planState(createdPlan)
        );

        return platformPlanMapper.toResponse(createdPlan);
    }

    private void validatePlanType(String planType, Integer trialDays) {
        if (!planType.equals("DEMO") && !planType.equals("PAID") && !planType.equals("ENTERPRISE")) {
            throw new BusinessException("planType must be DEMO, PAID or ENTERPRISE");
        }

        if (planType.equals("DEMO")) {
            if (trialDays == null || trialDays <= 0) {
                throw new BusinessException("trialDays must be greater than zero for DEMO plans");
            }
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }
}
