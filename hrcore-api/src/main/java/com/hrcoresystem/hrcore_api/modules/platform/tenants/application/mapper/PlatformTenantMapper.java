package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.mapper;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto.PlatformTenantResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import org.springframework.stereotype.Component;

@Component
public class PlatformTenantMapper {

    public PlatformTenantResponse toResponse(PlatformTenant tenant) {
        return new PlatformTenantResponse(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.schemaName(),
                tenant.status().name(),
                tenant.planId(),
                tenant.stripeCustomerId(),
                tenant.active(),
                tenant.createdAt(),
                tenant.updatedAt()
        );
    }
}
