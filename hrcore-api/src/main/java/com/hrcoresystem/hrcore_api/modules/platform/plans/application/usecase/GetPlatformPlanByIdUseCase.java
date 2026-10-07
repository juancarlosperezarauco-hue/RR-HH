package com.hrcoresystem.hrcore_api.modules.platform.plans.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.PlatformPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.mapper.PlatformPlanMapper;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.exception.PlatformPlanNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetPlatformPlanByIdUseCase {

    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformPlanMapper platformPlanMapper;

    public GetPlatformPlanByIdUseCase(
            PlatformPlanRepository platformPlanRepository,
            PlatformPlanMapper platformPlanMapper
    ) {
        this.platformPlanRepository = platformPlanRepository;
        this.platformPlanMapper = platformPlanMapper;
    }

    public PlatformPlanResponse execute(UUID id) {
        return platformPlanRepository.findById(id)
                .map(platformPlanMapper::toResponse)
                .orElseThrow(() -> new PlatformPlanNotFoundException("Platform plan not found with id: " + id));
    }
}