package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.PositionResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.PositionPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.PositionFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListPositionsUseCase {
    private final PositionRepository positions;
    private final PositionPolicy policy;
    private final PositionMapper mapper;
    public ListPositionsUseCase(PositionRepository positions, PositionPolicy policy, PositionMapper mapper) {
        this.positions = positions; this.policy = policy; this.mapper = mapper;
    }
    public Page<PositionResponse> execute(PositionFilter filter, Pageable pageable) {
        policy.assertOperational();
        return positions.findAll(filter, pageable).map(mapper::toResponse);
    }
}
