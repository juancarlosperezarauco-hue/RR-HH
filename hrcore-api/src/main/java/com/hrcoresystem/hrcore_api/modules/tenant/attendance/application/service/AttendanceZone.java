package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import java.time.*;
import org.springframework.stereotype.Component;

@Component
public class AttendanceZone {
    private final TenantSettingRepository settings;
    public AttendanceZone(TenantSettingRepository settings) { this.settings = settings; }
    public ZoneId required() {
        String name = settings.findByKey(TenantSettingKeys.COMPANY_TIMEZONE)
                .map(v -> v.settingValue()).map(String::trim).orElse("");
        if (!ZoneId.getAvailableZoneIds().contains(name))
            throw new BusinessException("Configure company.timezone with a valid IANA time zone");
        return ZoneId.of(name);
    }
    public Instant uniqueInstant(LocalDateTime local, ZoneId zone) {
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1) throw new IndeterminateScheduleTime(local);
        return local.atOffset(offsets.getFirst()).toInstant();
    }
    public static final class IndeterminateScheduleTime extends RuntimeException {
        public IndeterminateScheduleTime(LocalDateTime local) { super("Ambiguous or nonexistent local time: " + local); }
    }
}
