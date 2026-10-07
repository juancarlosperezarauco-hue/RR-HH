package com.hrcoresystem.hrcore_api.modules.platform.billing.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.dto.BillingConfigurationStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.billing.application.usecase.GetBillingConfigurationStatusUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/billing")
@SecurityRequirement(name = "bearerAuth")
public class PlatformBillingController {

    private final GetBillingConfigurationStatusUseCase getBillingConfigurationStatusUseCase;

    public PlatformBillingController(GetBillingConfigurationStatusUseCase getBillingConfigurationStatusUseCase) {
        this.getBillingConfigurationStatusUseCase = getBillingConfigurationStatusUseCase;
    }

    @GetMapping("/configuration-status")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<BillingConfigurationStatusResponse> configurationStatus() {
        return ApiResponse.success(
                "Billing configuration status retrieved successfully",
                getBillingConfigurationStatusUseCase.execute()
        );
    }
}
