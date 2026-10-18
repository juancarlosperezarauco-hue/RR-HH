package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.mapper.TenantSettingsMapper;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantSettingsValidationService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class UpdateTenantSettingsUseCase {
    private final TenantSettingRepository repository; private final TenantSettingsMapper mapper; private final AuditTrailService audit;
    private final TenantSettingsValidationService validation; private final TenantSettingsDependencyRepository dependencies;
    public UpdateTenantSettingsUseCase(TenantSettingRepository repository,TenantSettingsMapper mapper,AuditTrailService audit,TenantSettingsValidationService validation,TenantSettingsDependencyRepository dependencies){this.repository=repository;this.mapper=mapper;this.audit=audit;this.validation=validation;this.dependencies=dependencies;}
    @Transactional public TenantSettingsResponse execute(UpdateTenantSettingsRequest request){
        repository.requireForUpdate(TenantSettingKeys.HR_SETUP_COMPLETED); List<TenantSetting> before=repository.findAll();
        String oldTimezone=value(before,TenantSettingKeys.COMPANY_TIMEZONE),oldCurrency=value(before,TenantSettingKeys.COMPANY_CURRENCY),oldCountry=value(before,TenantSettingKeys.COMPANY_COUNTRY);
        String timezone=validation.normalizeTimezone(request.timezone()),currency=validation.normalizeCurrency(request.currency()); String country=request.country()==null?oldCountry:validation.normalizeCountry(request.country());
        if(!Objects.equals(oldTimezone,timezone)&&dependencies.hasOperationalDateDependencies())throw new BusinessConflictException("No se puede cambiar la zona horaria porque existen registros laborales dependientes");
        if((!Objects.equals(oldCurrency,currency)||!Objects.equals(oldCountry,country))&&dependencies.hasContracts())throw new BusinessConflictException("No se puede cambiar país o moneda porque existen contratos laborales");
        saveIfChanged(TenantSettingKeys.COMPANY_NAME,requiredName(request.companyName())); saveIfChanged(TenantSettingKeys.COMPANY_LEGAL_NAME,nullable(request.legalName())); saveIfChanged(TenantSettingKeys.COMPANY_TIMEZONE,timezone); saveIfChanged(TenantSettingKeys.COMPANY_CURRENCY,currency); saveIfChanged(TenantSettingKeys.COMPANY_CONTACT_EMAIL,nullable(request.contactEmail())); saveIfChanged(TenantSettingKeys.COMPANY_CONTACT_PHONE,nullable(request.contactPhone()));
        if(request.country()!=null)saveIfChanged(TenantSettingKeys.COMPANY_COUNTRY,country); if(request.address()!=null)saveIfChanged(TenantSettingKeys.COMPANY_ADDRESS,nullable(request.address())); if(request.taxIdentifier()!=null)saveIfChanged(TenantSettingKeys.COMPANY_TAX_IDENTIFIER,nullable(request.taxIdentifier()));
        if(!Objects.equals(oldTimezone,timezone)||!Objects.equals(oldCurrency,currency)||!Objects.equals(oldCountry,country))saveIfChanged(TenantSettingKeys.HR_SETUP_COMPLETED,"false");
        List<TenantSetting> after=repository.findAll(); List<String> changed=after.stream().filter(s->!Objects.equals(value(before,s.settingKey()),s.settingValue())).map(TenantSetting::settingKey).toList(); if(!changed.isEmpty())audit.recordTenantEvent(AuditEventTypes.TENANT_SETTINGS_UPDATED,"TENANT_SETTINGS","GLOBAL",PlatformAuditPayloads.details("updatedKeys",changed),PlatformAuditPayloads.settingsState(before),PlatformAuditPayloads.settingsState(after)); return mapper.toResponse(after);
    }
    private void saveIfChanged(String key,String value){TenantSetting old=repository.findByKey(key).orElse(null);if(old!=null&&Objects.equals(old.settingValue(),value))return;repository.save(new TenantSetting(old==null?null:old.id(),key,value,old==null?null:old.createdAt(),old==null?null:old.updatedAt()));}
    private String requiredName(String value){String v=nullable(value);if(v==null||v.length()>150)throw new BusinessException("El nombre de la empresa es obligatorio");return v;}
    private String nullable(String value){if(value==null)return null;String v=value.trim();return v.isEmpty()?null:v;}
    private String value(List<TenantSetting> all,String key){return all.stream().filter(s->key.equals(s.settingKey())).map(TenantSetting::settingValue).findFirst().orElse(null);}
}
