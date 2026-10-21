package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantRegionalFormatsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantRegionalFormatsUseCaseTest {
    @Test
    void getUsesDefaultsWithoutWriting() {
        var repository = new MemoryRepository();
        var result = new GetTenantRegionalFormatsUseCase(repository).execute();
        assertEquals("DMY", result.dateFormat());
        assertEquals("DECIMAL_COMMA", result.numberFormat());
        assertEquals(0, repository.saves);
    }

    @Test
    void putPersistsOnlyFormatsAndGetReturnsThem() {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        repository.put(TenantSettingKeys.HR_SETUP_COMPLETED, "true");
        repository.put(TenantSettingKeys.COMPANY_COUNTRY, "BO");
        repository.put(TenantSettingKeys.COMPANY_CURRENCY, "BOB");
        repository.put(TenantSettingKeys.COMPANY_TIMEZONE, "America/La_Paz");
        var result = new UpdateTenantRegionalFormatsUseCase(repository, audit)
                .execute(new UpdateTenantRegionalFormatsRequest("YMD", "DECIMAL_POINT"));
        assertEquals("YMD", result.dateFormat());
        assertEquals("DECIMAL_POINT", result.numberFormat());
        assertEquals(result, new GetTenantRegionalFormatsUseCase(repository).execute());
        assertEquals(2, repository.saves);
        assertEquals(1, audit.events);
        assertEquals("true", repository.value(TenantSettingKeys.HR_SETUP_COMPLETED));
        assertEquals("BO", repository.value(TenantSettingKeys.COMPANY_COUNTRY));
        assertEquals("BOB", repository.value(TenantSettingKeys.COMPANY_CURRENCY));
        assertEquals("America/La_Paz", repository.value(TenantSettingKeys.COMPANY_TIMEZONE));
    }

    @Test
    void unchangedFormatsDoNotWriteOrAudit() {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        repository.put(TenantSettingKeys.HR_SETUP_COMPLETED, "true");
        repository.put(TenantSettingKeys.REGIONAL_DATE_FORMAT, "MDY");
        repository.put(TenantSettingKeys.REGIONAL_NUMBER_FORMAT, "DECIMAL_POINT");
        new UpdateTenantRegionalFormatsUseCase(repository, audit)
                .execute(new UpdateTenantRegionalFormatsRequest("MDY", "DECIMAL_POINT"));
        assertEquals(0, repository.saves);
        assertEquals(0, audit.events);
    }

    @Test
    void invalidFormatsAreRejectedBeforeAnyWrite() {
        var repository = new MemoryRepository();
        var audit = new RecordingAudit();
        var useCase = new UpdateTenantRegionalFormatsUseCase(repository, audit);
        assertThrows(BusinessException.class,
                () -> useCase.execute(new UpdateTenantRegionalFormatsRequest("dmy", "DECIMAL_COMMA")));
        assertThrows(BusinessException.class,
                () -> useCase.execute(new UpdateTenantRegionalFormatsRequest("DMY", "UNKNOWN")));
        assertThrows(BusinessException.class, () -> useCase.execute(null));
        assertEquals(0, repository.saves);
        assertEquals(0, audit.events);
    }

    private static class MemoryRepository implements TenantSettingRepository {
        final Map<String, TenantSetting> rows = new HashMap<>();
        int saves;
        void put(String key, String value) { rows.put(key, new TenantSetting(UUID.randomUUID(), key, value, null, null)); }
        String value(String key) { return rows.get(key).settingValue(); }
        @Override public List<TenantSetting> findAll() { return List.copyOf(rows.values()); }
        @Override public Optional<TenantSetting> findByKey(String key) { return Optional.ofNullable(rows.get(key)); }
        @Override public TenantSetting requireForUpdate(String key) { return findByKey(key).orElseThrow(); }
        @Override public TenantSetting save(TenantSetting setting) {
            saves++;
            rows.put(setting.settingKey(), setting);
            return setting;
        }
    }

    private static class RecordingAudit extends AuditTrailService {
        int events;
        RecordingAudit() { super(null, null, null, null); }
        @Override public void recordTenantEvent(String eventType, String resourceType, String resourceId,
                                                Object details, Object beforeState, Object afterState) { events++; }
    }
}
