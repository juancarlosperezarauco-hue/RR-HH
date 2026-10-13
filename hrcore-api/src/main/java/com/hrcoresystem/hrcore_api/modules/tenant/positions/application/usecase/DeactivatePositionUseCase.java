package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.PositionResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DeactivatePositionUseCase {
    private final PositionRepository positions;
    private final PositionPolicy policy;
    private final PositionAudit audit;
    private final PositionMapper mapper;
    public DeactivatePositionUseCase(PositionRepository positions, PositionPolicy policy, PositionAudit audit, PositionMapper mapper) {
        this.positions = positions; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public PositionResponse execute(UUID id) {
        policy.assertOperational();
        Position before = policy.requirePositionForUpdate(id);
        if (before.status() == PositionStatus.INACTIVE) return mapper.toResponse(before);
        if (positions.hasCurrentAssignments(id)) throw new BusinessException("Position has current employee assignments");
        Position saved = positions.save(new Position(id, before.code(), before.name(), before.description(),
                before.responsibilities(), before.organizationalLevel(), before.salaryMin(), before.salaryMax(),
                before.currency(), PositionStatus.INACTIVE, before.createdBy(), policy.actorId(),
                before.createdAt(), before.updatedAt()));
        audit.position("DEACTIVATED", before, saved);
        return mapper.toResponse(saved);
    }
}
