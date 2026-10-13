package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AttendanceZoneTest {
    @Test
    void requiresIanaZoneAndRejectsDstGapsAndOverlaps() {
        TenantSettingRepository settings = mock(TenantSettingRepository.class);
        AttendanceZone zone = new AttendanceZone(settings);
        assertThrows(BusinessException.class, zone::required);
        when(settings.findByKey(TenantSettingKeys.COMPANY_TIMEZONE)).thenReturn(Optional.of(
                new TenantSetting(null, TenantSettingKeys.COMPANY_TIMEZONE, "America/New_York", null, null)));
        ZoneId newYork = zone.required();
        assertThrows(AttendanceZone.IndeterminateScheduleTime.class,
                () -> zone.uniqueInstant(LocalDateTime.of(2026, 3, 8, 2, 30), newYork));
        assertThrows(AttendanceZone.IndeterminateScheduleTime.class,
                () -> zone.uniqueInstant(LocalDateTime.of(2026, 11, 1, 1, 30), newYork));
        assertEquals(Instant.parse("2026-01-12T13:00:00Z"),
                zone.uniqueInstant(LocalDateTime.of(2026, 1, 12, 8, 0), newYork));
    }
}
