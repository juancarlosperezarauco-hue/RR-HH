package com.hrcoresystem.hrcore_api.common.tenancy.interceptor;

import com.hrcoresystem.hrcore_api.common.tenancy.TenancyProperties;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.common.tenancy.resolver.TenantResolver;
import com.hrcoresystem.hrcore_api.common.tenancy.validation.TenantSchemaReadinessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private final TenancyProperties tenancyProperties;
    private final TenantResolver tenantResolver;
    private final TenantSchemaReadinessService tenantSchemaReadinessService;

    public TenantContextInterceptor(
            TenancyProperties tenancyProperties,
            TenantResolver tenantResolver,
            TenantSchemaReadinessService tenantSchemaReadinessService
    ) {
        this.tenancyProperties = tenancyProperties;
        this.tenantResolver = tenantResolver;
        this.tenantSchemaReadinessService = tenantSchemaReadinessService;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        String requestPath = request.getRequestURI();

        if (tenancyProperties.usesPublicSchema(requestPath)) {
            TenantContextHolder.set(new TenantContext(
                    null,
                    tenancyProperties.getPublicSchema(),
                    true
            ));
            return true;
        }

        TenantContext resolvedTenant = tenantResolver.resolve(request);
        tenantSchemaReadinessService.assertTenantSchemaReady(
                resolvedTenant.schemaName(),
                tenancyProperties.getHeaderName(),
                resolvedTenant.tenantSlug()
        );
        TenantContextHolder.set(resolvedTenant);
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        TenantContextHolder.clear();
    }
}
