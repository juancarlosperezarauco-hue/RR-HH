package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantLanguageResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantLanguageRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UpdateTenantLanguageUseCase {
    private static final Set<String> LANGUAGES = Set.of(
            "es", "en", "pt-BR", "fr", "de", "it", "zh-Hans", "zh-Hant",
            "ja", "ko", "ru", "ar", "hi", "tr", "id");
    private final TenantSettingRepository repository;
    private final AuditTrailService audit;

    public Set<String> supportedLanguages() {
        return LANGUAGES;
    }

    public UpdateTenantLanguageUseCase(TenantSettingRepository repository, AuditTrailService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional
    public TenantLanguageResponse execute(UpdateTenantLanguageRequest request) {
        if (request == null || !LANGUAGES.contains(request.language())) {
            throw new BusinessException("El idioma empresarial no es válido");
        }
        // The existing tenant row serializes concurrent updates without changing its value.
        repository.requireForUpdate(TenantSettingKeys.HR_SETUP_COMPLETED);
        TenantSetting current = repository.findByKey(TenantSettingKeys.REGIONAL_LANGUAGE).orElse(null);
        if (current == null || !Objects.equals(current.settingValue(), request.language())) {
            repository.save(new TenantSetting(
                    current == null ? null : current.id(),
                    TenantSettingKeys.REGIONAL_LANGUAGE,
                    request.language(),
                    current == null ? null : current.createdAt(),
                    current == null ? null : current.updatedAt()
            ));
            audit.recordTenantEvent(AuditEventTypes.TENANT_SETTINGS_UPDATED,
                    "TENANT_LANGUAGE", "GLOBAL",
                    Map.of("updatedKey", TenantSettingKeys.REGIONAL_LANGUAGE),
                    new TenantLanguageResponse(current == null ? "es" : current.settingValue()),
                    new TenantLanguageResponse(request.language()));
        }
        return new TenantLanguageResponse(request.language());
    }
}
