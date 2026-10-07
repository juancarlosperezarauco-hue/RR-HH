package com.hrcoresystem.hrcore_api.modules.platform.dashboard.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.dto.TenantSummaryResponse;
import com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.usecase.GetTenantSummaryUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final GetTenantSummaryUseCase getTenantSummaryUseCase;

    public DashboardController(GetTenantSummaryUseCase getTenantSummaryUseCase) {
        this.getTenantSummaryUseCase = getTenantSummaryUseCase;
    }

    @GetMapping("/tenant/summary")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TenantSummaryResponse> getTenantSummary() {
        String tenantSlug = TenantContextHolder.getRequired().tenantSlug();

        return ApiResponse.success(
                "Tenant summary retrieved successfully",
                getTenantSummaryUseCase.execute(tenantSlug)
        );
    }
}
