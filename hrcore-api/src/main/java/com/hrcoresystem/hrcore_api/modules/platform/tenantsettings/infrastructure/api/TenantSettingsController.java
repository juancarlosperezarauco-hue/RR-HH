package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantSettingsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetTenantSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateTenantSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateHrSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.CompleteTenantSetupUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantSetupStatusService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.*;
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
    private final UpdateHrSettingsUseCase updateHrSettingsUseCase;
    private final TenantSetupStatusService tenantSetupStatusService;
    private final CompleteTenantSetupUseCase completeTenantSetupUseCase;

    public TenantSettingsController(
            GetTenantSettingsUseCase getTenantSettingsUseCase,
            UpdateTenantSettingsUseCase updateTenantSettingsUseCase, UpdateHrSettingsUseCase updateHrSettingsUseCase,
            TenantSetupStatusService tenantSetupStatusService, CompleteTenantSetupUseCase completeTenantSetupUseCase
    ) {
        this.getTenantSettingsUseCase = getTenantSettingsUseCase;
        this.updateTenantSettingsUseCase = updateTenantSettingsUseCase;
        this.updateHrSettingsUseCase = updateHrSettingsUseCase;
        this.tenantSetupStatusService = tenantSetupStatusService;
        this.completeTenantSetupUseCase = completeTenantSetupUseCase;
    }

    @GetMapping
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.read')")
    public ApiResponse<TenantSettingsResponse> getTenantSettings() {
        return ApiResponse.success(
                "Configuración empresarial consultada correctamente",
                getTenantSettingsUseCase.execute()
        );
    }

    @PutMapping
    @PreAuthorize("hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<TenantSettingsResponse> updateTenantSettings(
            @Valid @RequestBody UpdateTenantSettingsRequest request
    ) {
        return ApiResponse.success(
                "Configuración empresarial actualizada correctamente",
                updateTenantSettingsUseCase.execute(request)
        );
    }

    @GetMapping("/hr") @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.read')")
    public ApiResponse<HrSettingsResponse> getHrSettings(){ return ApiResponse.success("Configuración de RR. HH. consultada correctamente", updateHrSettingsUseCase.response()); }
    @PutMapping("/hr") @PreAuthorize("hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<HrSettingsResponse> updateHrSettings(@Valid @RequestBody UpdateHrSettingsRequest request){ return ApiResponse.success("Configuración de RR. HH. actualizada correctamente", updateHrSettingsUseCase.execute(request)); }
    @GetMapping("/setup-status") @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.read')")
    public ApiResponse<TenantSetupStatusResponse> setupStatus(){ return ApiResponse.success("Estado de configuración consultado correctamente", tenantSetupStatusService.execute()); }
    @PostMapping("/complete-setup") @PreAuthorize("hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<TenantSetupStatusResponse> completeSetup(@Valid @RequestBody CompleteTenantSetupRequest request){ return ApiResponse.success("Configuración de empresa completada correctamente", completeTenantSetupUseCase.execute(request)); }
}
