package com.hrcoresystem.hrcore_api.modules.tenant.positions.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.dto.PositionResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.mapper.PositionMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.application.service.PositionPolicy;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class GetPositionUseCase {
    private final PositionPolicy policy;
    private final PositionMapper mapper;
    public GetPositionUseCase(PositionPolicy policy, PositionMapper mapper) { this.policy = policy; this.mapper = mapper; }
    public PositionResponse execute(UUID id) {
        policy.assertOperational();
        return mapper.toResponse(policy.requirePosition(id));
    }
}
