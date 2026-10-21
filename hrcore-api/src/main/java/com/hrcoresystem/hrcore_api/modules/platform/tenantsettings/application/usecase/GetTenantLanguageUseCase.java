package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantLanguageResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTenantLanguageUseCase {
    private final TenantSettingRepository repository;

    public GetTenantLanguageUseCase(TenantSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TenantLanguageResponse execute() {
        String language = repository.findByKey(TenantSettingKeys.REGIONAL_LANGUAGE)
                .map(setting -> setting.settingValue())
                .orElse("es");
        return new TenantLanguageResponse(language);
    }
}
