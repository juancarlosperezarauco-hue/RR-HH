package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantSettingsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetTenantSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateTenantSettingsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/tenant")
@SecurityRequirement(name = "bearerAuth")
public class TenantSettingsController {

    private final GetTenantSettingsUseCase getTenantSettingsUseCase;
    private final UpdateTenantSettingsUseCase updateTenantSettingsUseCase;

    public TenantSettingsController(
            GetTenantSettingsUseCase getTenantSettingsUseCase,
            UpdateTenantSettingsUseCase updateTenantSettingsUseCase
    ) {
        this.getTenantSettingsUseCase = getTenantSettingsUseCase;
        this.updateTenantSettingsUseCase = updateTenantSettingsUseCase;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TenantSettingsResponse> getTenantSettings() {
        return ApiResponse.success(
                "Tenant settings retrieved successfully",
                getTenantSettingsUseCase.execute()
        );
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TenantSettingsResponse> updateTenantSettings(
            @Valid @RequestBody UpdateTenantSettingsRequest request
    ) {
        return ApiResponse.success(
                "Tenant settings updated successfully",
                updateTenantSettingsUseCase.execute(request)
        );
    }
}
