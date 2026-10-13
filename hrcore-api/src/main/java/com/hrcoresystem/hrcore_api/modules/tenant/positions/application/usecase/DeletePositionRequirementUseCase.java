package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.PositionRequirement;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DeletePositionRequirementUseCase {
    private final PositionRequirementRepository requirements;
    private final PositionPolicy policy;
    private final PositionAudit audit;
    public DeletePositionRequirementUseCase(PositionRequirementRepository requirements, PositionPolicy policy, PositionAudit audit) {
        this.requirements = requirements; this.policy = policy; this.audit = audit;
    }
    @Transactional
    public void execute(UUID positionId, UUID requirementId) {
        policy.assertOperational();
        policy.requirePositionForUpdate(positionId);
        PositionRequirement before = policy.requireRequirement(positionId, requirementId);
        requirements.delete(before);
        audit.requirement("DELETED", before, null);
    }
}
