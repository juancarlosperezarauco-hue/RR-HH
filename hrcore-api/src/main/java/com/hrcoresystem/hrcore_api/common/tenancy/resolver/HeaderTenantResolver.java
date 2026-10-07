package com.hrcoresystem.hrcore_api.common.tenancy.resolver;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.exception.TenantResolutionException;
import com.hrcoresystem.hrcore_api.common.tenancy.schema.TenantSchemaNamingStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class HeaderTenantResolver implements TenantResolver {

    private final TenancyProperties tenancyProperties;
    private final TenantSchemaNamingStrategy tenantSchemaNamingStrategy;

    public HeaderTenantResolver(
            TenancyProperties tenancyProperties,
            TenantSchemaNamingStrategy tenantSchemaNamingStrategy
    ) {
        this.tenancyProperties = tenancyProperties;
        this.tenantSchemaNamingStrategy = tenantSchemaNamingStrategy;
    }

    @Override
    public TenantContext resolve(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        if (tenancyProperties.isPublicPath(requestPath) || tenancyProperties.isGlobalPath(requestPath)) {
            return new TenantContext(
                    null,
                    tenancyProperties.getPublicSchema(),
                    true
            );
        }

        String rawTenantSlug = request.getHeader(tenancyProperties.getHeaderName());

        if (!StringUtils.hasText(rawTenantSlug)) {
            throw new TenantResolutionException(
                    "Required tenant header '" + tenancyProperties.getHeaderName() + "' is missing"
            );
        }

        String normalizedSlug = tenantSchemaNamingStrategy.normalizeSlug(rawTenantSlug);

        String schemaName = tenantSchemaNamingStrategy.toSchemaName(normalizedSlug);

        return new TenantContext(normalizedSlug, schemaName, false);
    }
}
