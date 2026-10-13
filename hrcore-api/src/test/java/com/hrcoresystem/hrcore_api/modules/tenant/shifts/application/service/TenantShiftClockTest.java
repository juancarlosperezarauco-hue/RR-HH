package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TenantShiftClockTest {
    private final TenantSettingRepository settings = mock(TenantSettingRepository.class);
    private final TenantShiftClock clock = new TenantShiftClock(settings);

    @Test
    void requiresConfiguredIanaZone() {
        when(settings.findByKey(TenantSettingKeys.COMPANY_TIMEZONE)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, clock::today);

        when(settings.findByKey(TenantSettingKeys.COMPANY_TIMEZONE))
                .thenReturn(Optional.of(new TenantSetting(null, TenantSettingKeys.COMPANY_TIMEZONE,
                        "+04:00", null, null)));
        assertThrows(BusinessException.class, clock::today);
    }

    @Test
    void usesTenantsCivilDate() {
        when(settings.findByKey(TenantSettingKeys.COMPANY_TIMEZONE))
                .thenReturn(Optional.of(new TenantSetting(null, TenantSettingKeys.COMPANY_TIMEZONE,
                        "America/La_Paz", null, null)));
        assertEquals(LocalDate.now(ZoneId.of("America/La_Paz")), clock.today());
    }
}
