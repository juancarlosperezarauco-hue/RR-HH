package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.mapper.TenantSettingsMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class GetTenantSettingsUseCase {

    private final TenantSettingRepository tenantSettingRepository;
    private final TenantSettingsMapper tenantSettingsMapper;

    public GetTenantSettingsUseCase(
            TenantSettingRepository tenantSettingRepository,
            TenantSettingsMapper tenantSettingsMapper
    ) {
        this.tenantSettingRepository = tenantSettingRepository;
        this.tenantSettingsMapper = tenantSettingsMapper;
    }

    public TenantSettingsResponse execute() {
        return tenantSettingsMapper.toResponse(tenantSettingRepository.findAll());
    }
}