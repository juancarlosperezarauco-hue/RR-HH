package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantSettingsValidationService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantSetupStatusService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class CompleteTenantSetupUseCase {
    private final TenantSettingRepository repo; private final TenantSettingsValidationService validation; private final TenantSetupStatusService status; private final AuditTrailService audit;
    public CompleteTenantSetupUseCase(TenantSettingRepository r,TenantSettingsValidationService v,TenantSetupStatusService s,AuditTrailService a){repo=r;validation=v;status=s;audit=a;}
    @Transactional public TenantSetupStatusResponse execute(CompleteTenantSetupRequest request){
        TenantSetting lock=repo.requireForUpdate(TenantSettingKeys.HR_SETUP_COMPLETED);
        String timezone=repo.findByKey(TenantSettingKeys.COMPANY_TIMEZONE).map(TenantSetting::settingValue).orElse("");
        if(request.confirmedTimezone()==null||!timezone.equals(request.confirmedTimezone()))throw new BusinessException("La zona horaria confirmada no coincide con la configuración actual");
        var missing=validation.missingFields(); if(!missing.isEmpty())throw new InvalidBusinessStateException("Debe completar los datos empresariales y las políticas de RR. HH.");
        boolean wasCompleted="true".equals(lock.settingValue()); if(!wasCompleted){repo.save(new TenantSetting(lock.id(),lock.settingKey(),"true",lock.createdAt(),lock.updatedAt()));audit.recordTenantEvent("TENANT_HR_SETUP_COMPLETED","TENANT_SETTINGS","GLOBAL",PlatformAuditPayloads.details("confirmedTimezone",timezone));}
        return status.execute();
    }
}
