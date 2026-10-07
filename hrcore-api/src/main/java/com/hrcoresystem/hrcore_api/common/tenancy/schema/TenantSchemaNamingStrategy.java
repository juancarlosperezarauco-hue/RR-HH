package com.hrcoresystem.hrcore_api.common.tenancy.schema;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantSchemaNamingStrategy {

    private final TenancyProperties tenancyProperties;

    public TenantSchemaNamingStrategy(TenancyProperties tenancyProperties) {
        this.tenancyProperties = tenancyProperties;
    }

    public String normalizeSlug(String rawSlug) {
        if (!StringUtils.hasText(rawSlug)) {
            throw new TenantResolutionException("Tenant slug header is required");
        }

        String normalized = rawSlug.trim()
                .toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9\\-_]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("_{2,}", "_")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");

        if (!StringUtils.hasText(normalized)) {
            throw new TenantResolutionException("Tenant slug is invalid");
        }

        if (!normalized.matches("^[a-z0-9][a-z0-9\\-_]{1,62}$")) {
            throw new TenantResolutionException("Tenant slug format is invalid");
        }

        return normalized;
    }

    public String toSchemaName(String tenantSlug) {
        String normalizedSlug = normalizeSlug(tenantSlug);
        String safeSchemaSuffix = normalizedSlug.replace("-", "_");
        return tenancyProperties.getTenantSchemaPrefix() + safeSchemaSuffix;
    }
}