package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantLanguageRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantLanguageUseCaseTest {
    @Test
    void getDefaultsToSpanishWithoutWriting() {
        var repository = new MemoryRepository();
        assertEquals("es", new GetTenantLanguageUseCase(repository).execute().language());
        assertEquals(0, repository.saves);
    }

    @Test
    void getReturnsPersistedLanguage() {
        var repository = new MemoryRepository();
        repository.rows.put(TenantSettingKeys.REGIONAL_LANGUAGE, setting(TenantSettingKeys.REGIONAL_LANGUAGE, "en"));
        assertEquals("en", new GetTenantLanguageUseCase(repository).execute().language());
    }

    @Test
    void putWritesOnlyLanguageAndLeavesExistingSettingsUntouched() {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        repository.rows.put(TenantSettingKeys.COMPANY_COUNTRY, setting(TenantSettingKeys.COMPANY_COUNTRY, "BO"));
        repository.rows.put(TenantSettingKeys.COMPANY_CURRENCY, setting(TenantSettingKeys.COMPANY_CURRENCY, "BOB"));
        repository.rows.put(TenantSettingKeys.COMPANY_TIMEZONE, setting(TenantSettingKeys.COMPANY_TIMEZONE, "America/La_Paz"));
        repository.rows.put(TenantSettingKeys.HR_SETUP_COMPLETED, setting(TenantSettingKeys.HR_SETUP_COMPLETED, "true"));

        assertEquals("en", new UpdateTenantLanguageUseCase(repository, audit)
                .execute(new UpdateTenantLanguageRequest("en")).language());
        assertEquals(1, repository.saves);
        assertEquals(TenantSettingKeys.REGIONAL_LANGUAGE, repository.lastSaved.settingKey());
        assertEquals("en", repository.lastSaved.settingValue());
        assertEquals("BO", repository.value(TenantSettingKeys.COMPANY_COUNTRY));
        assertEquals("BOB", repository.value(TenantSettingKeys.COMPANY_CURRENCY));
        assertEquals("America/La_Paz", repository.value(TenantSettingKeys.COMPANY_TIMEZONE));
        assertEquals("true", repository.value(TenantSettingKeys.HR_SETUP_COMPLETED));
        assertEquals(1, audit.events);
    }

    @Test
    void putAcceptsSpanishAndSkipsUnchangedValue() {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        repository.rows.put(TenantSettingKeys.HR_SETUP_COMPLETED, setting(TenantSettingKeys.HR_SETUP_COMPLETED, "true"));
        repository.rows.put(TenantSettingKeys.REGIONAL_LANGUAGE, setting(TenantSettingKeys.REGIONAL_LANGUAGE, "es"));
        assertEquals("es", new UpdateTenantLanguageUseCase(repository, audit)
                .execute(new UpdateTenantLanguageRequest("es")).language());
        assertEquals(0, repository.saves);
        assertEquals(0, audit.events);
    }

    @Test
    void putRejectsUnsupportedLanguageWithoutWriting() {
        var repository = new MemoryRepository();
        assertThrows(BusinessException.class, () -> new UpdateTenantLanguageUseCase(repository, new RecordingAudit())
                .execute(new UpdateTenantLanguageRequest("pt")));
        assertEquals(0, repository.saves);
    }

    @ParameterizedTest
    @ValueSource(strings = {"es", "en", "pt-BR", "fr", "de", "it", "zh-Hans", "zh-Hant",
            "ja", "ko", "ru", "ar", "hi", "tr", "id"})
    void putAcceptsEachApprovedLanguageWithoutChangingOtherSettings(String language) {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        repository.rows.put(TenantSettingKeys.HR_SETUP_COMPLETED, setting(TenantSettingKeys.HR_SETUP_COMPLETED, "true"));
        repository.rows.put(TenantSettingKeys.COMPANY_COUNTRY, setting(TenantSettingKeys.COMPANY_COUNTRY, "BO"));
        repository.rows.put(TenantSettingKeys.COMPANY_CURRENCY, setting(TenantSettingKeys.COMPANY_CURRENCY, "BOB"));
        repository.rows.put(TenantSettingKeys.COMPANY_TIMEZONE, setting(TenantSettingKeys.COMPANY_TIMEZONE, "America/La_Paz"));

        assertEquals(language, new UpdateTenantLanguageUseCase(repository, audit)
                .execute(new UpdateTenantLanguageRequest(language)).language());
        assertEquals(language, new GetTenantLanguageUseCase(repository).execute().language());
        assertEquals(1, repository.saves);
        assertEquals(TenantSettingKeys.REGIONAL_LANGUAGE, repository.lastSaved.settingKey());
        assertEquals(language, repository.lastSaved.settingValue());
        assertEquals("BO", repository.value(TenantSettingKeys.COMPANY_COUNTRY));
        assertEquals("BOB", repository.value(TenantSettingKeys.COMPANY_CURRENCY));
        assertEquals("America/La_Paz", repository.value(TenantSettingKeys.COMPANY_TIMEZONE));
        assertEquals("true", repository.value(TenantSettingKeys.HR_SETUP_COMPLETED));
        assertEquals(1, audit.events);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pt", "PT-BR", "pt-br", "zh-hans", "zh-HANT", "zh_Hans",
            "ES", " en", "en ", "", " "})
    void putRejectsUnapprovedSpellingWithoutWriting(String language) {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        assertThrows(BusinessException.class, () -> new UpdateTenantLanguageUseCase(repository, audit)
                .execute(new UpdateTenantLanguageRequest(language)));
        assertEquals(0, repository.saves);
        assertEquals(0, audit.events);
    }

    @Test
    void nullRequestIsRejectedWithoutWriting() {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        assertThrows(BusinessException.class, () -> new UpdateTenantLanguageUseCase(repository, audit).execute(null));
        assertEquals(0, repository.saves);
        assertEquals(0, audit.events);
    }

    @Test
    void nullAndBlankFieldsFailDtoValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new UpdateTenantLanguageRequest(null)).isEmpty());
            assertFalse(validator.validate(new UpdateTenantLanguageRequest("")).isEmpty());
            assertFalse(validator.validate(new UpdateTenantLanguageRequest(" ")).isEmpty());
        }
    }

    @Test
    void getPreservesStoredCompoundCodeWithoutWriting() {
        var repository = new MemoryRepository();
        repository.rows.put(TenantSettingKeys.REGIONAL_LANGUAGE,
                setting(TenantSettingKeys.REGIONAL_LANGUAGE, "zh-Hant"));
        assertEquals("zh-Hant", new GetTenantLanguageUseCase(repository).execute().language());
        assertEquals(0, repository.saves);
    }

    private static TenantSetting setting(String key, String value) {
        return new TenantSetting(UUID.randomUUID(), key, value, null, null);
    }

    private static class MemoryRepository implements TenantSettingRepository {
        final Map<String, TenantSetting> rows = new HashMap<>();
        int saves;
        TenantSetting lastSaved;

        @Override public List<TenantSetting> findAll() { return List.copyOf(rows.values()); }
        @Override public Optional<TenantSetting> findByKey(String key) { return Optional.ofNullable(rows.get(key)); }
        @Override public TenantSetting requireForUpdate(String key) { return findByKey(key).orElseThrow(); }
        @Override public TenantSetting save(TenantSetting setting) {
            saves++;
            lastSaved = setting;
            rows.put(setting.settingKey(), setting);
            return setting;
        }
        String value(String key) { return rows.get(key).settingValue(); }
    }

    private static class RecordingAudit extends AuditTrailService {
        int events;
        RecordingAudit() { super(null, null, null, null); }
        @Override public void recordTenantEvent(String eventType, String resourceType, String resourceId,
                                                 Object details, Object beforeState, Object afterState) { events++; }
    }
}
