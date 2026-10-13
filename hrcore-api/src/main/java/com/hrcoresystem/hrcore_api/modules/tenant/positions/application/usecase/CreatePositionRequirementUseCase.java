package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.PositionRequirement;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CreatePositionRequirementUseCase {
    private final PositionRequirementRepository requirements;
    private final PositionPolicy policy;
    private final PositionAudit audit;
    private final PositionMapper mapper;
    public CreatePositionRequirementUseCase(PositionRequirementRepository requirements, PositionPolicy policy,
                                            PositionAudit audit, PositionMapper mapper) {
        this.requirements = requirements; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public PositionRequirementResponse execute(UUID positionId, PositionRequirementRequest request) {
        policy.assertOperational();
        policy.requirePositionForUpdate(positionId);
        PositionRequirement saved = requirements.save(policy.normalizedRequirement(positionId, request, null));
        audit.requirement("CREATED", null, saved);
        return mapper.toResponse(saved);
    }
}
