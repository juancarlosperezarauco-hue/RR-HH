package com.hrcoresystem.hrcore_api.modules.platform.dashboard.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.dto.SuperadminDashboardResponse;
import com.hrcoresystem.hrcore_api.modules.platform.dashboard.application.usecase.GetSuperadminDashboardUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class PlatformDashboardController {

    private final GetSuperadminDashboardUseCase getSuperadminDashboardUseCase;

    public PlatformDashboardController(GetSuperadminDashboardUseCase getSuperadminDashboardUseCase) {
        this.getSuperadminDashboardUseCase = getSuperadminDashboardUseCase;
    }

    @GetMapping("/summary")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<SuperadminDashboardResponse> summary() {
        return ApiResponse.success(
                "Superadmin dashboard retrieved successfully",
                getSuperadminDashboardUseCase.execute()
        );
    }
}
