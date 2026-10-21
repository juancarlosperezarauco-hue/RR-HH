package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.TenantRegionalFormatsResponse;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.UpdateTenantRegionalFormatsRequest;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UpdateTenantRegionalFormatsUseCase {
    private static final Set<String> DATE_FORMATS = Set.of("DMY", "MDY", "YMD");
    private static final Set<String> NUMBER_FORMATS = Set.of("DECIMAL_COMMA", "DECIMAL_POINT");
    private final TenantSettingRepository repository;
    private final AuditTrailService audit;

    public UpdateTenantRegionalFormatsUseCase(TenantSettingRepository repository, AuditTrailService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional
    public TenantRegionalFormatsResponse execute(UpdateTenantRegionalFormatsRequest request) {
        if (request == null || !DATE_FORMATS.contains(request.dateFormat())
                || !NUMBER_FORMATS.contains(request.numberFormat())) {
            throw new BusinessException("Los formatos regionales no son válidos");
        }
        repository.requireForUpdate(TenantSettingKeys.HR_SETUP_COMPLETED);
        TenantRegionalFormatsResponse before = new GetTenantRegionalFormatsUseCase(repository).execute();
        List<String> changed = new ArrayList<>();
        saveIfChanged(TenantSettingKeys.REGIONAL_DATE_FORMAT, request.dateFormat(), changed);
        saveIfChanged(TenantSettingKeys.REGIONAL_NUMBER_FORMAT, request.numberFormat(), changed);
        TenantRegionalFormatsResponse after = new TenantRegionalFormatsResponse(request.dateFormat(), request.numberFormat());
        if (!changed.isEmpty()) {
            audit.recordTenantEvent(AuditEventTypes.TENANT_SETTINGS_UPDATED,
                    "TENANT_REGIONAL_FORMATS", "GLOBAL", Map.of("updatedKeys", changed), before, after);
        }
        return after;
    }

    private void saveIfChanged(String key, String value, List<String> changed) {
        TenantSetting current = repository.findByKey(key).orElse(null);
        if (current != null && Objects.equals(current.settingValue(), value)) return;
        repository.save(new TenantSetting(
                current == null ? null : current.id(), key, value,
                current == null ? null : current.createdAt(),
                current == null ? null : current.updatedAt()));
        changed.add(key);
    }
}
