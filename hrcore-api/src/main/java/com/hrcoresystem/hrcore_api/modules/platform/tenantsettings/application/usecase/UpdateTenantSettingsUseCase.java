package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantSettingsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantSettingsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.mapper.TenantSettingsMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UpdateTenantSettingsUseCase {

    private final TenantSettingRepository tenantSettingRepository;
    private final TenantSettingsMapper tenantSettingsMapper;
    private final AuditTrailService auditTrailService;

    public UpdateTenantSettingsUseCase(
            TenantSettingRepository tenantSettingRepository,
            TenantSettingsMapper tenantSettingsMapper,
            AuditTrailService auditTrailService
    ) {
        this.tenantSettingRepository = tenantSettingRepository;
        this.tenantSettingsMapper = tenantSettingsMapper;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public TenantSettingsResponse execute(UpdateTenantSettingsRequest request) {
        List<TenantSetting> beforeState = tenantSettingRepository.findAll();

        saveOrUpdate(TenantSettingKeys.COMPANY_NAME, normalizeRequired(request.companyName()));
        saveOrUpdate(TenantSettingKeys.COMPANY_LEGAL_NAME, normalizeNullable(request.legalName()));
        saveOrUpdate(TenantSettingKeys.COMPANY_TIMEZONE, normalizeRequired(request.timezone()));
        saveOrUpdate(TenantSettingKeys.COMPANY_CURRENCY, normalizeRequired(request.currency()).toUpperCase());
        saveOrUpdate(TenantSettingKeys.COMPANY_CONTACT_EMAIL, normalizeNullable(request.contactEmail()));
        saveOrUpdate(TenantSettingKeys.COMPANY_CONTACT_PHONE, normalizeNullable(request.contactPhone()));

        List<TenantSetting> afterState = tenantSettingRepository.findAll();

        auditTrailService.recordPlatformEvent(
                AuditEventTypes.TENANT_SETTINGS_UPDATED,
                "TENANT_SETTINGS",
                "GLOBAL",
                PlatformAuditPayloads.details(
                        "updatedKeys", List.of(
                                TenantSettingKeys.COMPANY_NAME,
                                TenantSettingKeys.COMPANY_LEGAL_NAME,
                                TenantSettingKeys.COMPANY_TIMEZONE,
                                TenantSettingKeys.COMPANY_CURRENCY,
                                TenantSettingKeys.COMPANY_CONTACT_EMAIL,
                                TenantSettingKeys.COMPANY_CONTACT_PHONE
                        )
                ),
                PlatformAuditPayloads.settingsState(beforeState),
                PlatformAuditPayloads.settingsState(afterState)
        );

        return tenantSettingsMapper.toResponse(afterState);
    }

    private void saveOrUpdate(String key, String value) {
        TenantSetting existing = tenantSettingRepository.findByKey(key).orElse(null);

        TenantSetting settingToSave = new TenantSetting(
                existing != null ? existing.id() : null,
                key,
                value,
                existing != null ? existing.createdAt() : null,
                existing != null ? existing.updatedAt() : null
        );

        tenantSettingRepository.save(settingToSave);
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
