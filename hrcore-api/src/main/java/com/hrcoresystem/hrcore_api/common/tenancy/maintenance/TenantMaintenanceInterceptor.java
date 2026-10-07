package com.hrcoresystem.hrcore_api.common.tenancy.maintenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.response.ApiErrorResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class TenantMaintenanceInterceptor implements HandlerInterceptor {

    private final TenantMaintenanceService tenantMaintenanceService;
    private final ObjectMapper objectMapper;

    public TenantMaintenanceInterceptor(
            TenantMaintenanceService tenantMaintenanceService,
            ObjectMapper objectMapper
    ) {
        this.tenantMaintenanceService = tenantMaintenanceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {
        TenantContext context;
        try {
            context = TenantContextHolder.getRequired();
        } catch (Exception exception) {
            return true;
        }

        if (context.publicRequest() || context.tenantSlug() == null) {
            return true;
        }

        if (!tenantMaintenanceService.isTenantInMaintenance(context.tenantSlug())) {
            return true;
        }

        if (request.getRequestURI().startsWith("/api/backups")) {
            return true;
        }

        String reason = tenantMaintenanceService.getMaintenanceReason(context.tenantSlug())
                .orElse("Tenant is temporarily unavailable due to maintenance");

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse body = ApiErrorResponse.of(
                "Tenant is temporarily unavailable due to maintenance",
                List.of(reason)
        );

        objectMapper.writeValue(response.getOutputStream(), body);
        return false;
    }
}
