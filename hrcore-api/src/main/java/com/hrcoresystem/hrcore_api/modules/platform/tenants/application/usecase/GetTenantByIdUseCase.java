package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.exception.PlatformTenantNotFoundException;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetTenantByIdUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformTenantMapper platformTenantMapper;

    public GetTenantByIdUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformTenantMapper platformTenantMapper
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformTenantMapper = platformTenantMapper;
    }

    public PlatformTenantResponse execute(UUID id) {
        return platformTenantRepository.findById(id)
                .map(platformTenantMapper::toResponse)
                .orElseThrow(() -> new PlatformTenantNotFoundException("Tenant not found with id: " + id));
    }
}