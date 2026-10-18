package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.audit.PlatformAuditPayloads;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service public class UpdateHrSettingsUseCase {
    private final TenantSettingRepository repo; private final AuditTrailService audit;
    private static final List<String> KEYS=List.of(TenantSettingKeys.ATTENDANCE_EMPLOYEE_WEB_PUNCH_ENABLED,TenantSettingKeys.ATTENDANCE_GEOLOCATION_REQUIRED,TenantSettingKeys.ATTENDANCE_BREAK_PUNCH_REQUIRED,TenantSettingKeys.ATTENDANCE_DAILY_PROCESSING_ENABLED,TenantSettingKeys.ATTENDANCE_ADJUSTMENT_APPROVAL_REQUIRED,TenantSettingKeys.LEAVE_SELF_SERVICE_ENABLED,TenantSettingKeys.LEAVE_SUPERVISOR_APPROVAL_ENABLED);
    public UpdateHrSettingsUseCase(TenantSettingRepository repo,AuditTrailService audit){this.repo=repo;this.audit=audit;}
    @Transactional public HrSettingsResponse execute(UpdateHrSettingsRequest r){
        repo.requireForUpdate(TenantSettingKeys.HR_SETUP_COMPLETED); Map<String,String> before=raw();
        if(!r.adjustmentApprovalRequired())throw new BusinessException("La aprobación de correcciones debe permanecer habilitada");
        save(TenantSettingKeys.ATTENDANCE_EMPLOYEE_WEB_PUNCH_ENABLED,r.employeeWebPunchEnabled()); save(TenantSettingKeys.ATTENDANCE_GEOLOCATION_REQUIRED,r.geolocationRequired()); save(TenantSettingKeys.ATTENDANCE_BREAK_PUNCH_REQUIRED,r.breakPunchRequired()); save(TenantSettingKeys.ATTENDANCE_DAILY_PROCESSING_ENABLED,r.dailyProcessingEnabled()); save(TenantSettingKeys.ATTENDANCE_ADJUSTMENT_APPROVAL_REQUIRED,true); save(TenantSettingKeys.LEAVE_SELF_SERVICE_ENABLED,r.leaveSelfServiceEnabled()); save(TenantSettingKeys.LEAVE_SUPERVISOR_APPROVAL_ENABLED,r.leaveSupervisorApprovalEnabled());
        Map<String,String> after=raw(); if(!before.equals(after))audit.recordTenantEvent("TENANT_HR_POLICIES_UPDATED","TENANT_SETTINGS","GLOBAL",PlatformAuditPayloads.details("before",before,"after",after)); return response();
    }
    public HrSettingsResponse response(){return new HrSettingsResponse(requiredBoolean(TenantSettingKeys.ATTENDANCE_EMPLOYEE_WEB_PUNCH_ENABLED),requiredBoolean(TenantSettingKeys.ATTENDANCE_GEOLOCATION_REQUIRED),requiredBoolean(TenantSettingKeys.ATTENDANCE_BREAK_PUNCH_REQUIRED),requiredBoolean(TenantSettingKeys.ATTENDANCE_DAILY_PROCESSING_ENABLED),requiredApproval(),requiredBoolean(TenantSettingKeys.LEAVE_SELF_SERVICE_ENABLED),requiredBoolean(TenantSettingKeys.LEAVE_SUPERVISOR_APPROVAL_ENABLED));}
    private boolean requiredApproval(){if(!requiredBoolean(TenantSettingKeys.ATTENDANCE_ADJUSTMENT_APPROVAL_REQUIRED))throw invalid();return true;}
    private boolean requiredBoolean(String key){String value=repo.findByKey(key).map(TenantSetting::settingValue).orElse(null);if(value==null||!("true".equalsIgnoreCase(value.trim())||"false".equalsIgnoreCase(value.trim())))throw invalid();return Boolean.parseBoolean(value.trim());}
    private InvalidBusinessStateException invalid(){return new InvalidBusinessStateException("La configuración de RR. HH. contiene valores inválidos");}
    private void save(String key,boolean value){TenantSetting old=repo.findByKey(key).orElse(new TenantSetting(null,key,null,null,null));if(Objects.equals(old.settingValue(),String.valueOf(value)))return;repo.save(new TenantSetting(old.id(),key,String.valueOf(value),old.createdAt(),old.updatedAt()));}
    private Map<String,String> raw(){Map<String,String> m=new LinkedHashMap<>();for(String key:KEYS)m.put(key,repo.findByKey(key).map(TenantSetting::settingValue).orElse(null));return m;}
}
