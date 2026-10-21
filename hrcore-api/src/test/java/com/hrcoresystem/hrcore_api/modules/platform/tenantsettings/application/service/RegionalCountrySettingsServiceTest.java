package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
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

class RegionalCountrySettingsServiceTest {
    @Test
    void holidaysAreSortedAndEditableByReplacingTheCountryList() {
        var repo = new MemoryRepository();
        var audit = new RecordingAudit();
        var service = service(repo, audit);
        var first = service.saveHolidays("BO", new RegionalCountrySettingsService.Holidays(List.of(
                new RegionalCountrySettingsService.Holiday("2026-12-25", "  Descanso  "),
                new RegionalCountrySettingsService.Holiday("2026-01-01", "Inicio"))));
        assertEquals(List.of("2026-01-01", "2026-12-25"),
                first.holidays().stream().map(RegionalCountrySettingsService.Holiday::date).toList());
        assertEquals("Descanso", first.holidays().get(1).name());
        assertEquals(first, service.holidays("BO"));
        assertEquals(1, repo.saves);
        service.saveHolidays("BO", first);
        assertEquals(1, repo.saves);
        var edited = service.saveHolidays("BO", new RegionalCountrySettingsService.Holidays(List.of(
                new RegionalCountrySettingsService.Holiday("2026-01-01", "Nombre editado"))));
        assertEquals(1, edited.holidays().size());
        assertEquals("Nombre editado", service.holidays("BO").holidays().getFirst().name());
        assertEquals(2, repo.saves);
        assertEquals(2, audit.events);
    }

    @Test
    void invalidCountryDateAndDuplicateHolidayNeverWrite() {
        var repo = new MemoryRepository();
        var service = service(repo, new RecordingAudit());
        assertThrows(BusinessException.class, () -> service.holidays("BO;DROP"));
        assertThrows(BusinessException.class, () -> service.saveHolidays("BO", new RegionalCountrySettingsService.Holidays(List.of(
                new RegionalCountrySettingsService.Holiday("2026-02-30", "Inválido")))));
        assertThrows(BusinessException.class, () -> service.saveHolidays("BO", new RegionalCountrySettingsService.Holidays(List.of(
                new RegionalCountrySettingsService.Holiday("2026-01-01", "Uno"),
                new RegionalCountrySettingsService.Holiday("2026-01-01", "Dos")))));
        assertEquals(0, repo.saves);
    }

    @Test
    void sameCountryInDifferentTenantsAndTwoCountriesStayIndependent() {
        var tenantA = new MemoryRepository();
        var tenantB = new MemoryRepository();
        var a = service(tenantA, new RecordingAudit());
        var b = service(tenantB, new RecordingAudit());
        a.saveHolidays("BO", new RegionalCountrySettingsService.Holidays(List.of(
                new RegionalCountrySettingsService.Holiday("2026-01-01", "A"))));
        a.saveHolidays("AR", new RegionalCountrySettingsService.Holidays(List.of(
                new RegionalCountrySettingsService.Holiday("2026-02-01", "Argentina"))));
        b.saveHolidays("BO", new RegionalCountrySettingsService.Holidays(List.of(
                new RegionalCountrySettingsService.Holiday("2026-03-01", "B"))));
        assertEquals("A", a.holidays("BO").holidays().getFirst().name());
        assertEquals("Argentina", a.holidays("AR").holidays().getFirst().name());
        assertEquals("B", b.holidays("BO").holidays().getFirst().name());
    }

    @Test
    void workdayCalculatesHoursAndRejectsInvalidBreak() {
        var repo = new MemoryRepository();
        var service = service(repo, new RecordingAudit());
        var saved = service.saveWorkday("BO", new RegionalCountrySettingsService.Workday(
                List.of("WEDNESDAY", "MONDAY", "TUESDAY"), "09:00", "18:00", "13:00", "14:00"));
        assertEquals(480, saved.dailyMinutes());
        assertEquals(1440, saved.weeklyMinutes());
        assertEquals(List.of("MONDAY", "TUESDAY", "WEDNESDAY"), saved.days());
        assertEquals(saved, service.workday("BO"));
        assertThrows(BusinessException.class, () -> service.saveWorkday("BO",
                new RegionalCountrySettingsService.Workday(List.of("MONDAY"), "09:00", "18:00", "08:00", "09:30")));
        assertEquals(1, repo.saves);
    }

    @Test
    void rulesRemainReferenceDataAndDoNotOverwriteOtherSections() {
        var repo = new MemoryRepository();
        var audit = new RecordingAudit();
        var service = service(repo, audit);
        repo.put(TenantSettingKeys.COMPANY_COUNTRY, "BO");
        repo.put(TenantSettingKeys.COMPANY_CURRENCY, "BOB");
        repo.put(TenantSettingKeys.COMPANY_TIMEZONE, "America/La_Paz");
        service.saveHolidays("BO", new RegionalCountrySettingsService.Holidays(List.of()));
        service.saveWorkday("BO", new RegionalCountrySettingsService.Workday(List.of("MONDAY"), "09:00", "17:00", null, null));
        var rules = new RegionalCountrySettingsService.Rules(30, 15, 5, 24);
        assertEquals(rules, service.saveRules("BO", rules));
        assertEquals(rules, service.rules("BO"));
        assertEquals(1, service.workday("BO").days().size());
        assertEquals("BO", repo.value(TenantSettingKeys.COMPANY_COUNTRY));
        assertEquals("BOB", repo.value(TenantSettingKeys.COMPANY_CURRENCY));
        assertEquals("America/La_Paz", repo.value(TenantSettingKeys.COMPANY_TIMEZONE));
        assertEquals("true", repo.value(TenantSettingKeys.HR_SETUP_COMPLETED));
        assertThrows(BusinessException.class, () -> service.saveRules("BO",
                new RegionalCountrySettingsService.Rules(366, null, null, null)));
        assertEquals(rules, service.rules("BO"));
        assertEquals(2, repo.saves);
        assertEquals(2, audit.events);
    }

    private static RegionalCountrySettingsService service(MemoryRepository repo, RecordingAudit audit) {
        return new RegionalCountrySettingsService(repo, audit, new ObjectMapper());
    }

    private static class MemoryRepository implements TenantSettingRepository {
        private final Map<String, TenantSetting> rows = new HashMap<>();
        int saves;
        MemoryRepository() { put(TenantSettingKeys.HR_SETUP_COMPLETED, "true"); }
        void put(String key, String value) { rows.put(key, new TenantSetting(UUID.randomUUID(), key, value, null, null)); }
        String value(String key) { return rows.get(key).settingValue(); }
        @Override public List<TenantSetting> findAll() { return List.copyOf(rows.values()); }
        @Override public Optional<TenantSetting> findByKey(String key) { return Optional.ofNullable(rows.get(key)); }
        @Override public TenantSetting requireForUpdate(String key) { return findByKey(key).orElseThrow(); }
        @Override public TenantSetting save(TenantSetting value) { saves++; rows.put(value.settingKey(), value); return value; }
    }

    private static class RecordingAudit extends AuditTrailService {
        int events;
        RecordingAudit() { super(null, null, null, null); }
        @Override public void recordTenantEvent(String type, String resource, String id,
                                                Object details, Object before, Object after) { events++; }
    }
}
