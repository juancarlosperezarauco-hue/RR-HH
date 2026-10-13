package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.PositionRequirementResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.PositionPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRequirementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ListPositionRequirementsUseCase {
    private final PositionRequirementRepository requirements;
    private final PositionPolicy policy;
    private final PositionMapper mapper;
    public ListPositionRequirementsUseCase(PositionRequirementRepository requirements, PositionPolicy policy, PositionMapper mapper) {
        this.requirements = requirements; this.policy = policy; this.mapper = mapper;
    }
    public Page<PositionRequirementResponse> execute(UUID positionId, Pageable pageable) {
        policy.assertOperational();
        policy.requirePosition(positionId);
        return requirements.findByPositionId(positionId, pageable).map(mapper::toResponse);
    }
}
