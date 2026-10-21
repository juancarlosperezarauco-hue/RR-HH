package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.security.authorization.TenantScopeGuard;
import com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.security.TenantOwnershipGuard;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantLanguageRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantRegionalFormatsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetTenantLanguageUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.GetTenantRegionalFormatsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateTenantLanguageUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase.UpdateTenantRegionalFormatsUseCase;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.RegionalCountrySettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantLanguageSecurityTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        TestConfig.tenantAllowed = false;
        TestConfig.ownerAllowed = false;
    }

    @Test
    void getAllowsTenantWithoutSettingsReadButRejectsOtherScopes() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var controller = context.getBean(TenantSettingsController.class);
            assertThrows(AccessDeniedException.class, controller::getTenantLanguage);
            authenticate();
            assertThrows(AccessDeniedException.class, controller::getTenantLanguage);
            TestConfig.tenantAllowed = true;
            assertDoesNotThrow(controller::getTenantLanguage);
        }
    }

    @Test
    void putRequiresUpdatePermissionAndCurrentOwner() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var controller = context.getBean(TenantSettingsController.class);
            var request = new UpdateTenantLanguageRequest("en");
            TestConfig.tenantAllowed = true;
            authenticate();
            assertThrows(AccessDeniedException.class, () -> controller.updateTenantLanguage(request));
            authenticate("tenant-settings.update");
            assertThrows(AccessDeniedException.class, () -> controller.updateTenantLanguage(request));
            TestConfig.ownerAllowed = true;
            assertDoesNotThrow(() -> controller.updateTenantLanguage(request));
        }
    }

    @Test
    void supportedLanguagesAreVisibleOnlyWithinTenant() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var controller = context.getBean(TenantSettingsController.class);
            authenticate();
            assertThrows(AccessDeniedException.class, controller::getSupportedTenantLanguages);
            TestConfig.tenantAllowed = true;
            assertEquals(15, controller.getSupportedTenantLanguages().data().size());
        }
    }

    @Test
    void regionalFormatsRequireTenantForGetAndOwnerPermissionForPut() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var controller = context.getBean(TenantSettingsController.class);
            authenticate();
            assertThrows(AccessDeniedException.class, controller::getTenantRegionalFormats);
            TestConfig.tenantAllowed = true;
            assertDoesNotThrow(controller::getTenantRegionalFormats);
            var request = new UpdateTenantRegionalFormatsRequest("YMD", "DECIMAL_POINT");
            assertThrows(AccessDeniedException.class, () -> controller.updateTenantRegionalFormats(request));
            authenticate("tenant-settings.update");
            assertThrows(AccessDeniedException.class, () -> controller.updateTenantRegionalFormats(request));
            TestConfig.ownerAllowed = true;
            assertDoesNotThrow(() -> controller.updateTenantRegionalFormats(request));
        }
    }

    @Test
    void countrySettingsRequireTenantReadAndOwnerUpdate() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var controller = context.getBean(TenantSettingsController.class);
            authenticate("tenant-settings.read");
            assertThrows(AccessDeniedException.class, () -> controller.getCountryHolidays("BO"));
            TestConfig.tenantAllowed = true;
            assertDoesNotThrow(() -> controller.getCountryHolidays("BO"));
            assertDoesNotThrow(() -> controller.getCountryWorkday("BO"));
            assertDoesNotThrow(() -> controller.getCountryRules("BO"));
            var holidays = new RegionalCountrySettingsService.Holidays(List.of());
            assertThrows(AccessDeniedException.class, () -> controller.saveCountryHolidays("BO", holidays));
            authenticate("tenant-settings.read", "tenant-settings.update");
            assertThrows(AccessDeniedException.class, () -> controller.saveCountryHolidays("BO", holidays));
            TestConfig.ownerAllowed = true;
            assertDoesNotThrow(() -> controller.saveCountryHolidays("BO", holidays));
        }
    }

    private void authenticate(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "tenant-user", "n/a", java.util.Arrays.stream(permissions)
                        .map(SimpleGrantedAuthority::new).toList()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfig {
        static boolean tenantAllowed;
        static boolean ownerAllowed;

        @Bean("tenantScopeGuard") TenantScopeGuard tenantScopeGuard() {
            return new TenantScopeGuard(null) {
                @Override public boolean isTenant() { return tenantAllowed; }
            };
        }
        @Bean("tenantOwnershipGuard") TenantOwnershipGuard tenantOwnershipGuard() {
            return new TenantOwnershipGuard(null) {
                @Override public boolean isCurrentOwner() { return ownerAllowed; }
            };
        }
        @Bean TenantSettingsController tenantSettingsController() {
            var repository = new MemoryRepository();
            var languageGet = new GetTenantLanguageUseCase(repository);
            var audit = new AuditTrailService(null, null, null, null) {
                @Override public void recordTenantEvent(String eventType, String resourceType, String resourceId,
                                                         Object details, Object beforeState, Object afterState) { }
            };
            var languagePut = new UpdateTenantLanguageUseCase(repository, audit);
            return new TenantSettingsController(null, null, null, null, null,
                    null, null, languageGet, languagePut,
                    new GetTenantRegionalFormatsUseCase(repository),
                    new UpdateTenantRegionalFormatsUseCase(repository, audit),
                    new RegionalCountrySettingsService(repository, audit, new ObjectMapper()));
        }
    }

    private static class MemoryRepository implements TenantSettingRepository {
        private final Map<String, TenantSetting> rows = new HashMap<>();
        MemoryRepository() {
            rows.put(TenantSettingKeys.HR_SETUP_COMPLETED,
                    new TenantSetting(UUID.randomUUID(), TenantSettingKeys.HR_SETUP_COMPLETED, "true", null, null));
        }
        @Override public List<TenantSetting> findAll() { return List.copyOf(rows.values()); }
        @Override public Optional<TenantSetting> findByKey(String key) { return Optional.ofNullable(rows.get(key)); }
        @Override public TenantSetting requireForUpdate(String key) { return findByKey(key).orElseThrow(); }
        @Override public TenantSetting save(TenantSetting setting) {
            rows.put(setting.settingKey(), setting);
            return setting;
        }
    }
}
