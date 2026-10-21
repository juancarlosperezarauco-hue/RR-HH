package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantSettingsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetTenantSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateTenantSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateHrSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.CompleteTenantSetupUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetAppearanceSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateAppearanceSettingsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetTenantLanguageUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateTenantLanguageUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetTenantRegionalFormatsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateTenantRegionalFormatsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.RegionalCountrySettingsService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantSetupStatusService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/api/settings/tenant")
@SecurityRequirement(name = "bearerAuth")
public class TenantSettingsController {

    private final GetTenantSettingsUseCase getTenantSettingsUseCase;
    private final UpdateTenantSettingsUseCase updateTenantSettingsUseCase;
    private final UpdateHrSettingsUseCase updateHrSettingsUseCase;
    private final TenantSetupStatusService tenantSetupStatusService;
    private final CompleteTenantSetupUseCase completeTenantSetupUseCase;
    private final GetAppearanceSettingsUseCase getAppearanceSettingsUseCase;
    private final UpdateAppearanceSettingsUseCase updateAppearanceSettingsUseCase;
    private final GetTenantLanguageUseCase getTenantLanguageUseCase;
    private final UpdateTenantLanguageUseCase updateTenantLanguageUseCase;
    private final GetTenantRegionalFormatsUseCase getTenantRegionalFormatsUseCase;
    private final UpdateTenantRegionalFormatsUseCase updateTenantRegionalFormatsUseCase;
    private final RegionalCountrySettingsService regionalCountrySettingsService;

    public TenantSettingsController(
            GetTenantSettingsUseCase getTenantSettingsUseCase,
            UpdateTenantSettingsUseCase updateTenantSettingsUseCase, UpdateHrSettingsUseCase updateHrSettingsUseCase,
            TenantSetupStatusService tenantSetupStatusService, CompleteTenantSetupUseCase completeTenantSetupUseCase,
            GetAppearanceSettingsUseCase getAppearanceSettingsUseCase,
            UpdateAppearanceSettingsUseCase updateAppearanceSettingsUseCase,
            GetTenantLanguageUseCase getTenantLanguageUseCase,
            UpdateTenantLanguageUseCase updateTenantLanguageUseCase,
            GetTenantRegionalFormatsUseCase getTenantRegionalFormatsUseCase,
            UpdateTenantRegionalFormatsUseCase updateTenantRegionalFormatsUseCase,
            RegionalCountrySettingsService regionalCountrySettingsService
    ) {
        this.getTenantSettingsUseCase = getTenantSettingsUseCase;
        this.updateTenantSettingsUseCase = updateTenantSettingsUseCase;
        this.updateHrSettingsUseCase = updateHrSettingsUseCase;
        this.tenantSetupStatusService = tenantSetupStatusService;
        this.completeTenantSetupUseCase = completeTenantSetupUseCase;
        this.getAppearanceSettingsUseCase = getAppearanceSettingsUseCase;
        this.updateAppearanceSettingsUseCase = updateAppearanceSettingsUseCase;
        this.getTenantLanguageUseCase = getTenantLanguageUseCase;
        this.updateTenantLanguageUseCase = updateTenantLanguageUseCase;
        this.getTenantRegionalFormatsUseCase = getTenantRegionalFormatsUseCase;
        this.updateTenantRegionalFormatsUseCase = updateTenantRegionalFormatsUseCase;
        this.regionalCountrySettingsService = regionalCountrySettingsService;
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

    @GetMapping("/appearance")
    @PreAuthorize("@tenantScopeGuard.isTenant()")
    public ApiResponse<AppearanceSettingsResponse> getAppearanceSettings() {
        return ApiResponse.success("Apariencia empresarial consultada correctamente", getAppearanceSettingsUseCase.execute());
    }

    @GetMapping("/language")
    @PreAuthorize("@tenantScopeGuard.isTenant()")
    public ApiResponse<TenantLanguageResponse> getTenantLanguage() {
        return ApiResponse.success("Idioma empresarial consultado correctamente", getTenantLanguageUseCase.execute());
    }

    @GetMapping("/language/supported")
    @PreAuthorize("@tenantScopeGuard.isTenant()")
    public ApiResponse<Set<String>> getSupportedTenantLanguages() {
        return ApiResponse.success("Idiomas empresariales disponibles", updateTenantLanguageUseCase.supportedLanguages());
    }

    @PutMapping("/language")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<TenantLanguageResponse> updateTenantLanguage(@Valid @RequestBody UpdateTenantLanguageRequest request) {
        return ApiResponse.success("Idioma empresarial actualizado correctamente", updateTenantLanguageUseCase.execute(request));
    }

    @GetMapping("/regional-formats")
    @PreAuthorize("@tenantScopeGuard.isTenant()")
    public ApiResponse<TenantRegionalFormatsResponse> getTenantRegionalFormats() {
        return ApiResponse.success("Formatos regionales consultados correctamente", getTenantRegionalFormatsUseCase.execute());
    }

    @PutMapping("/regional-formats")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<TenantRegionalFormatsResponse> updateTenantRegionalFormats(
            @Valid @RequestBody UpdateTenantRegionalFormatsRequest request) {
        return ApiResponse.success("Formatos regionales actualizados correctamente", updateTenantRegionalFormatsUseCase.execute(request));
    }

    @GetMapping("/regional/countries/{country}/holidays")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.read')")
    public ApiResponse<RegionalCountrySettingsService.Holidays> getCountryHolidays(@PathVariable String country) {
        return ApiResponse.success("Feriados regionales consultados", regionalCountrySettingsService.holidays(country));
    }

    @PutMapping("/regional/countries/{country}/holidays")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<RegionalCountrySettingsService.Holidays> saveCountryHolidays(
            @PathVariable String country, @RequestBody RegionalCountrySettingsService.Holidays request) {
        return ApiResponse.success("Feriados regionales guardados", regionalCountrySettingsService.saveHolidays(country, request));
    }

    @GetMapping("/regional/countries/{country}/workday")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.read')")
    public ApiResponse<RegionalCountrySettingsService.WorkdayView> getCountryWorkday(@PathVariable String country) {
        return ApiResponse.success("Jornada regional consultada", regionalCountrySettingsService.workday(country));
    }

    @PutMapping("/regional/countries/{country}/workday")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<RegionalCountrySettingsService.WorkdayView> saveCountryWorkday(
            @PathVariable String country, @RequestBody RegionalCountrySettingsService.Workday request) {
        return ApiResponse.success("Jornada regional guardada", regionalCountrySettingsService.saveWorkday(country, request));
    }

    @GetMapping("/regional/countries/{country}/rules")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.read')")
    public ApiResponse<RegionalCountrySettingsService.Rules> getCountryRules(@PathVariable String country) {
        return ApiResponse.success("Parámetros regionales consultados", regionalCountrySettingsService.rules(country));
    }

    @PutMapping("/regional/countries/{country}/rules")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<RegionalCountrySettingsService.Rules> saveCountryRules(
            @PathVariable String country, @RequestBody RegionalCountrySettingsService.Rules request) {
        return ApiResponse.success("Parámetros regionales guardados", regionalCountrySettingsService.saveRules(country, request));
    }

    @PutMapping("/appearance")
    @PreAuthorize("@tenantScopeGuard.isTenant() and hasAuthority('tenant-settings.update') and @tenantOwnershipGuard.isCurrentOwner()")
    public ApiResponse<AppearanceSettingsResponse> updateAppearanceSettings(
            @Valid @RequestBody UpdateAppearanceSettingsRequest request
    ) {
        return ApiResponse.success("Apariencia empresarial actualizada correctamente", updateAppearanceSettingsUseCase.execute(request));
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
