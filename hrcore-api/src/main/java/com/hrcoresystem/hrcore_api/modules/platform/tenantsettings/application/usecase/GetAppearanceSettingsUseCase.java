package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.AppearanceSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class GetAppearanceSettingsUseCase {
    static final Set<String> PRESETS = Set.of("original", "corporate", "modern", "natural", "minimal", "warm");
    static final Set<String> MODES = Set.of("light", "dark");
    static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private final TenantSettingRepository repository;

    public GetAppearanceSettingsUseCase(TenantSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AppearanceSettingsResponse execute() {
        return fromSettings(repository.findAll());
    }

    static AppearanceSettingsResponse fromSettings(List<TenantSetting> settings) {
        Map<String, String> values = new HashMap<>();
        settings.forEach(setting -> values.put(setting.settingKey(), setting.settingValue()));
        return new AppearanceSettingsResponse(
                allowed(values.get(TenantSettingKeys.APPEARANCE_PRESET), PRESETS, "original"),
                color(values.get(TenantSettingKeys.APPEARANCE_PRIMARY), "#1268c4"),
                color(values.get(TenantSettingKeys.APPEARANCE_SECONDARY), "#13a8e8"),
                color(values.get(TenantSettingKeys.APPEARANCE_SIDEBAR), "#082d55"),
                allowed(values.get(TenantSettingKeys.APPEARANCE_MODE), MODES, "light")
        );
    }

    private static String allowed(String value, Set<String> accepted, String fallback) {
        return value != null && accepted.contains(value) ? value : fallback;
    }

    private static String color(String value, String fallback) {
        return value != null && HEX_COLOR.matcher(value).matches() ? value.toLowerCase(java.util.Locale.ROOT) : fallback;
    }
}
