package com.hrcoresystem.hrcore_api.common.config;

import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Applies subscription entitlements before controller authorization/handling. */
@Component
public class TenantPlanModuleInterceptor implements HandlerInterceptor {
    private final TenantPlanEnforcementService plans;

    public TenantPlanModuleInterceptor(TenantPlanEnforcementService plans) {
        this.plans = plans;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (matches(path, "/api/incidents") || matches(path, "/api/me/incidents")) {
            plans.assertModuleIncluded("incidents");
        } else if (matches(path, "/api/leaves") || matches(path, "/api/me/leaves")
                || matches(path, "/api/me/leave-balances")) {
            plans.assertModuleIncluded("leaves");
        } else if (matches(path, "/api/shifts") || matches(path, "/api/me/schedule")) {
            plans.assertModuleIncluded("shifts");
        } else if (matches(path, "/api/attendance") || matches(path, "/api/me/attendance")) {
            plans.assertModuleIncluded("attendance");
        } else if (matches(path, "/api/hr/reports") || matches(path, "/api/hr/ai")) {
            plans.assertModuleIncluded("hr.reports");
        }
        return true;
    }

    private boolean matches(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
