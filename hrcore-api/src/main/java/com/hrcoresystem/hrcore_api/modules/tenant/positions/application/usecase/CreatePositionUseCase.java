package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.Position;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.BusinessSequenceService;

@Service
public class CreatePositionUseCase {
    private final PositionRepository positions;
    private final PositionPolicy policy;
    private final PositionAudit audit;
    private final PositionMapper mapper;
    private final BusinessSequenceService sequences;

    public CreatePositionUseCase(PositionRepository positions, PositionPolicy policy, PositionAudit audit, PositionMapper mapper, BusinessSequenceService sequences) {
        this.positions = positions; this.policy = policy; this.audit = audit; this.mapper = mapper; this.sequences = sequences;
    }

    @Transactional
    public PositionResponse execute(PositionRequest request) {
        policy.assertOperational();
        Position saved = positions.save(policy.normalizedPosition(null, sequences.nextCode("position"), request, null, policy.actorId()));
        audit.position("CREATED", null, saved);
        return mapper.toResponse(saved);
    }
}
