package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** Supplies the tenant's civil date for operations whose rules depend on "today". */
@Component
public class TenantShiftClock {

    private final TenantSettingRepository settings;

    public TenantShiftClock(TenantSettingRepository settings) {
        this.settings = settings;
    }

    public LocalDate today() {
        String configured = settings.findByKey(TenantSettingKeys.COMPANY_TIMEZONE)
                .map(setting -> setting.settingValue())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new BusinessException(
                        "Configure company.timezone with a valid IANA time zone before managing shifts"));
        try {
            if (!ZoneId.getAvailableZoneIds().contains(configured)) {
                throw new BusinessException("company.timezone must be a valid IANA time zone");
            }
            return LocalDate.now(ZoneId.of(configured));
        } catch (DateTimeException exception) {
            throw new BusinessException("company.timezone must be a valid IANA time zone", exception);
        }
    }
}
