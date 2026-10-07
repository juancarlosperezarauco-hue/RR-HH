package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.mapper;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TenantSettingsMapper {

    public TenantSettingsResponse toResponse(List<TenantSetting> settings) {
        Map<String, String> settingsByKey = settings.stream()
                .collect(Collectors.toMap(
                        TenantSetting::settingKey,
                        TenantSetting::settingValue,
                        (first, second) -> second
                ));

        return new TenantSettingsResponse(
                settingsByKey.get(TenantSettingKeys.COMPANY_NAME),
                settingsByKey.get(TenantSettingKeys.COMPANY_LEGAL_NAME),
                settingsByKey.get(TenantSettingKeys.COMPANY_TIMEZONE),
                settingsByKey.get(TenantSettingKeys.COMPANY_CURRENCY),
                settingsByKey.get(TenantSettingKeys.COMPANY_CONTACT_EMAIL),
                settingsByKey.get(TenantSettingKeys.COMPANY_CONTACT_PHONE)
        );
    }
}