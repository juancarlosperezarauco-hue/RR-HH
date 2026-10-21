package com.hrcoresystem.hrcore_api.modules.governance.audit.application.dto;

import java.time.Instant;

public record TenantAuditFilterCriteria(
        Instant startDate,
        Instant endDate,
        String actor,
        String module,
        String action,
        String resourceType,
        String search
) {
    public static TenantAuditFilterCriteria empty() {
        return new TenantAuditFilterCriteria(null, null, null, null, null, null, null);
    }
}
