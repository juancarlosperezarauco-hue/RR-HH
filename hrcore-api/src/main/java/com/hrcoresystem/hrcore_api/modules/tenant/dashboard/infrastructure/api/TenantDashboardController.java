package com.hrcoresystem.hrcore_api.modules.tenant.dashboard.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.dashboard.application.dto.TenantDashboardResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.dashboard.application.usecase.GetTenantDashboardUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class TenantDashboardController {

    private final GetTenantDashboardUseCase useCase;

    public TenantDashboardController(GetTenantDashboardUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/summary")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('dashboard.tenant.summary.read')")
    public ApiResponse<TenantDashboardResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ApiResponse.success(
                "Dashboard del tenant generado correctamente",
                useCase.execute(fromDate, toDate)
        );
    }
}
