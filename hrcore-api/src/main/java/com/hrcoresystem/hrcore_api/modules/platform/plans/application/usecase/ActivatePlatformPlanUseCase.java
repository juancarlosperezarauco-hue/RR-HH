package com.hrcoresystem.hrcore_api.modules.platform.plans.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.PlatformPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.mapper.PlatformPlanMapper;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.exception.PlatformPlanNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivatePlatformPlanUseCase {

    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformPlanMapper platformPlanMapper;
    private final AuditTrailService auditTrailService;

    public ActivatePlatformPlanUseCase(
            PlatformPlanRepository platformPlanRepository,
            PlatformPlanMapper platformPlanMapper,
            AuditTrailService auditTrailService
    ) {
        this.platformPlanRepository = platformPlanRepository;
        this.platformPlanMapper = platformPlanMapper;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PlatformPlanResponse execute(UUID id) {
        PlatformPlan plan = platformPlanRepository.findById(id)
                .orElseThrow(() -> new PlatformPlanNotFoundException("Platform plan not found with id: " + id));
        PlatformPlan beforeState = plan;

        PlatformPlan updated = new PlatformPlan(
                plan.id(),
                plan.code(),
                plan.name(),
                plan.description(),
                plan.maxUsers(),
                plan.maxRoles(),
                plan.planType(),
                plan.trialDays(),
                true,
                plan.createdAt(),
                plan.updatedAt()
        );

        PlatformPlan saved = platformPlanRepository.save(updated);

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.PLATFORM_PLAN_ACTIVATED,
                "PLATFORM_PLAN",
                saved.id().toString(),
                PlatformAuditPayloads.details(
                        "code", saved.code(),
                        "active", saved.active()
                ),
                PlatformAuditPayloads.planState(beforeState),
                PlatformAuditPayloads.planState(saved)
        );

        return platformPlanMapper.toResponse(saved);
    }
}
