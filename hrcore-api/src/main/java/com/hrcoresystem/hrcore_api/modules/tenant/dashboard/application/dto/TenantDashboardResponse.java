package com.hrcoresystem.hrcore_api.modules.tenant.dashboard.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Permission-aware dashboard contract. Sections are omitted when the current
 * tenant user does not have the permission required to read their data.
 */
public record TenantDashboardResponse(
        Metadata metadata,
        List<Metric> metrics,
        List<Section> sections
) {

    public record Metadata(
            Instant generatedAt,
            String timezone,
            String tenantSlug,
            String generatedBy,
            LocalDate fromDate,
            LocalDate toDate
    ) { }

    public record Metric(
            String key,
            String label,
            Object value,
            String sourcePermission
    ) { }

    public record Section(
            String key,
            String title,
            String sourcePermission,
            Map<String, Object> data
    ) { }
}
