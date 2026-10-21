package com.hrcoresystem.hrcore_api.modules.platform.tenants.application.dto;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;

public record PlatformTenantFilterCriteria(
        String search,
        String name,
        String taxIdentifier,
        String plan,
        PlatformTenantStatus status,
        Boolean active
) {
    public static PlatformTenantFilterCriteria empty() {
        return new PlatformTenantFilterCriteria(null, null, null, null, null, null);
    }
}
