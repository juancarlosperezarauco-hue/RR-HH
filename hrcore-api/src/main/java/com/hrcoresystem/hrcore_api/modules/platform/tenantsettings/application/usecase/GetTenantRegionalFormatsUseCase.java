package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantRegionalFormatsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTenantRegionalFormatsUseCase {
    private final TenantSettingRepository repository;

    public GetTenantRegionalFormatsUseCase(TenantSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public TenantRegionalFormatsResponse execute() {
        return new TenantRegionalFormatsResponse(
                repository.findByKey(TenantSettingKeys.REGIONAL_DATE_FORMAT)
                        .map(setting -> setting.settingValue()).orElse("DMY"),
                repository.findByKey(TenantSettingKeys.REGIONAL_NUMBER_FORMAT)
                        .map(setting -> setting.settingValue()).orElse("DECIMAL_COMMA"));
    }
}
