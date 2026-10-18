package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantSetupStatusResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
@Service public class TenantSetupStatusService { private final TenantSettingRepository repo; private final TenantSettingsValidationService validation; public TenantSetupStatusService(TenantSettingRepository r,TenantSettingsValidationService v){repo=r;validation=v;} public TenantSetupStatusResponse execute(){var missing=validation.missingFields(); boolean completed=repo.findByKey(TenantSettingKeys.HR_SETUP_COMPLETED).map(s->"true".equals(s.settingValue())).orElse(false); return new TenantSetupStatusResponse(completed&&missing.isEmpty(),missing);} }
