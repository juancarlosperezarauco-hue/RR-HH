package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.Position;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class UpdatePositionUseCase {
    private final PositionRepository positions;
    private final PositionPolicy policy;
    private final PositionAudit audit;
    private final PositionMapper mapper;
    public UpdatePositionUseCase(PositionRepository positions, PositionPolicy policy, PositionAudit audit, PositionMapper mapper) {
        this.positions = positions; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public PositionResponse execute(UUID id, PositionRequest request) {
        policy.assertOperational();
        Position before = policy.requirePositionForUpdate(id);
        Position after = policy.normalizedPosition(id, request, before, policy.actorId());
        if (policy.sameContent(before, after)) return mapper.toResponse(before);
        Position saved = positions.save(after);
        audit.position("UPDATED", before, saved);
        return mapper.toResponse(saved);
    }
}
