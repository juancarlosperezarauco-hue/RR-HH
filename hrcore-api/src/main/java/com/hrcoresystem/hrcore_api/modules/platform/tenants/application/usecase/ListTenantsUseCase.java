package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantFilterCriteria;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper.PlatformTenantMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ListTenantsUseCase {

    private final PlatformTenantRepository platformTenantRepository;
    private final PlatformTenantMapper platformTenantMapper;
    private final PlatformPlanRepository platformPlanRepository;

    public ListTenantsUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformTenantMapper platformTenantMapper
    ) {
        this(platformTenantRepository, platformTenantMapper, null);
    }

    @Autowired
    public ListTenantsUseCase(
            PlatformTenantRepository platformTenantRepository,
            PlatformTenantMapper platformTenantMapper,
            PlatformPlanRepository platformPlanRepository
    ) {
        this.platformTenantRepository = platformTenantRepository;
        this.platformTenantMapper = platformTenantMapper;
        this.platformPlanRepository = platformPlanRepository;
    }

    public List<PlatformTenantResponse> execute() {
        return execute(PlatformTenantFilterCriteria.empty());
    }

    public List<PlatformTenantResponse> execute(PlatformTenantFilterCriteria criteria) {
        if (criteria == null) {
            criteria = PlatformTenantFilterCriteria.empty();
        }

        UUID planId = null;
        if (criteria.plan() != null && !criteria.plan().isBlank()) {
            String planVal = criteria.plan().trim();
            try {
                planId = UUID.fromString(planVal);
            } catch (IllegalArgumentException e) {
                if (platformPlanRepository != null) {
                    var planOpt = platformPlanRepository.findByCode(planVal.toUpperCase());
                    if (planOpt.isPresent()) {
                        planId = planOpt.get().id();
                    }
                }
            }
        }

        String search = (criteria.search() != null && !criteria.search().isBlank()) ? criteria.search().trim() : null;
        String name = (criteria.name() != null && !criteria.name().isBlank()) ? criteria.name().trim() : null;
        String taxIdentifier = (criteria.taxIdentifier() != null && !criteria.taxIdentifier().isBlank()) ? criteria.taxIdentifier().trim() : null;

        return platformTenantRepository.searchTenants(
                        search,
                        name,
                        taxIdentifier,
                        criteria.status(),
                        criteria.active(),
                        planId
                )
                .stream()
                .map(platformTenantMapper::toResponse)
                .toList();
    }
}
