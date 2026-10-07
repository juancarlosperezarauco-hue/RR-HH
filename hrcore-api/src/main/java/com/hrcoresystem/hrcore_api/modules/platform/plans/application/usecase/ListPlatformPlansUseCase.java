package com.hrcoresystem.hrcore_api.modules.platform.plans.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.application.dto.PlatformPlanResponse;
import com.hrcoresystem.hrcore_api.modules.platform.plans.application.mapper.PlatformPlanMapper;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListPlatformPlansUseCase {

    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformPlanMapper platformPlanMapper;

    public ListPlatformPlansUseCase(
            PlatformPlanRepository platformPlanRepository,
            PlatformPlanMapper platformPlanMapper
    ) {
        this.platformPlanRepository = platformPlanRepository;
        this.platformPlanMapper = platformPlanMapper;
    }

    public List<PlatformPlanResponse> execute() {
        return platformPlanRepository.findAll()
                .stream()
                .map(platformPlanMapper::toResponse)
                .toList();
    }
}