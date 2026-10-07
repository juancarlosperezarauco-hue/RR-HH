package com.hrcoresystem.hrcore_api.modules.platform.superadmin.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.application.dto.PlatformSuperadminResponse;
import com.hrcoresystem.hrcore_api.modules.platform.superadmin.application.usecase.GetCurrentPlatformSuperadminUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/superadmins")
@SecurityRequirement(name = "bearerAuth")
public class PlatformSuperadminController {

    private final GetCurrentPlatformSuperadminUseCase getCurrentPlatformSuperadminUseCase;

    public PlatformSuperadminController(
            GetCurrentPlatformSuperadminUseCase getCurrentPlatformSuperadminUseCase
    ) {
        this.getCurrentPlatformSuperadminUseCase = getCurrentPlatformSuperadminUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("@authorizationGuards.isPlatformAuthenticated()")
    public ApiResponse<PlatformSuperadminResponse> me() {
        return ApiResponse.success(
                "Platform superadmin retrieved successfully",
                getCurrentPlatformSuperadminUseCase.execute()
        );
    }
}
