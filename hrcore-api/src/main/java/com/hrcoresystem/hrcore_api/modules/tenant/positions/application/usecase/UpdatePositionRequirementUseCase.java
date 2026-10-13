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
public class UpdatePositionRequirementUseCase {
    private final PositionRequirementRepository requirements;
    private final PositionPolicy policy;
    private final PositionAudit audit;
    private final PositionMapper mapper;
    public UpdatePositionRequirementUseCase(PositionRequirementRepository requirements, PositionPolicy policy,
                                            PositionAudit audit, PositionMapper mapper) {
        this.requirements = requirements; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public PositionRequirementResponse execute(UUID positionId, UUID requirementId, PositionRequirementRequest request) {
        policy.assertOperational();
        policy.requirePositionForUpdate(positionId);
        PositionRequirement before = policy.requireRequirement(positionId, requirementId);
        PositionRequirement after = policy.normalizedRequirement(positionId, request, before);
        if (policy.sameContent(before, after)) return mapper.toResponse(before);
        PositionRequirement saved = requirements.save(after);
        audit.requirement("UPDATED", before, saved);
        return mapper.toResponse(saved);
    }
}
