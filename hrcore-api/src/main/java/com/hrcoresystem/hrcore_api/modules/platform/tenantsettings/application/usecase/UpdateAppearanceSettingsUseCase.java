package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.AppearanceSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateAppearanceSettingsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UpdateAppearanceSettingsUseCase {
    private final TenantSettingRepository repository;
    private final AuditTrailService audit;

    public UpdateAppearanceSettingsUseCase(TenantSettingRepository repository, AuditTrailService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional
    public AppearanceSettingsResponse execute(UpdateAppearanceSettingsRequest request) {
        AppearanceSettingsResponse validated = validate(request);
        // V3 crea esta fila para cada tenant. Su bloqueo serializa las cinco claves de apariencia.
        repository.requireForUpdate(TenantSettingKeys.HR_SETUP_COMPLETED);
        List<TenantSetting> before = repository.findAll();
        List<String> changed = new ArrayList<>();
        saveIfChanged(before, TenantSettingKeys.APPEARANCE_PRESET, validated.preset(), changed);
        saveIfChanged(before, TenantSettingKeys.APPEARANCE_PRIMARY, validated.primary(), changed);
        saveIfChanged(before, TenantSettingKeys.APPEARANCE_SECONDARY, validated.secondary(), changed);
        saveIfChanged(before, TenantSettingKeys.APPEARANCE_SIDEBAR, validated.sidebar(), changed);
        saveIfChanged(before, TenantSettingKeys.APPEARANCE_MODE, validated.mode(), changed);
        AppearanceSettingsResponse confirmed = GetAppearanceSettingsUseCase.fromSettings(repository.findAll());
        if (!changed.isEmpty()) {
            audit.recordTenantEvent(
                    AuditEventTypes.TENANT_SETTINGS_UPDATED,
                    "TENANT_APPEARANCE",
                    "GLOBAL",
                    PlatformAuditPayloads.details("updatedKeys", changed),
                    GetAppearanceSettingsUseCase.fromSettings(before),
                    confirmed
            );
        }
        return confirmed;
    }

    private AppearanceSettingsResponse validate(UpdateAppearanceSettingsRequest request) {
        if (request == null || !GetAppearanceSettingsUseCase.PRESETS.contains(request.preset())
                || !GetAppearanceSettingsUseCase.MODES.contains(request.mode())
                || !validColor(request.primary()) || !validColor(request.secondary())
                || !validColor(request.sidebar())) {
            throw new BusinessException("La apariencia contiene valores inválidos");
        }
        return new AppearanceSettingsResponse(
                request.preset(),
                request.primary().toLowerCase(Locale.ROOT),
                request.secondary().toLowerCase(Locale.ROOT),
                request.sidebar().toLowerCase(Locale.ROOT),
                request.mode()
        );
    }

    private boolean validColor(String value) {
        return value != null && GetAppearanceSettingsUseCase.HEX_COLOR.matcher(value).matches();
    }

    private void saveIfChanged(List<TenantSetting> before, String key, String value, List<String> changed) {
        TenantSetting current = before.stream().filter(setting -> key.equals(setting.settingKey())).findFirst().orElse(null);
        if (current != null && Objects.equals(current.settingValue(), value)) return;
        repository.save(new TenantSetting(
                current == null ? null : current.id(),
                key,
                value,
                current == null ? null : current.createdAt(),
                current == null ? null : current.updatedAt()
        ));
        changed.add(key);
    }
}
