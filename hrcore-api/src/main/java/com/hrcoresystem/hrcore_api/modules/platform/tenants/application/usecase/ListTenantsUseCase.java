package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListTenantsUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformTenantMapper platformTenantMapper;

    public ListTenantsUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformTenantMapper platformTenantMapper
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformTenantMapper = platformTenantMapper;
    }

    public List<PlatformTenantResponse> execute() {
        return platformTenantRepository.findAll()
                .stream()
                .map(platformTenantMapper::toResponse)
                .toList();
    }
}