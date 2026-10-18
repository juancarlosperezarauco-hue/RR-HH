package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.InvalidBusinessStateException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSetting;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;
import java.time.ZoneId;
import java.util.*;

@Service
public class TenantSettingsValidationService {
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());
    private static final List<String> POLICY_KEYS = List.of(
            TenantSettingKeys.ATTENDANCE_EMPLOYEE_WEB_PUNCH_ENABLED,
            TenantSettingKeys.ATTENDANCE_GEOLOCATION_REQUIRED,
            TenantSettingKeys.ATTENDANCE_BREAK_PUNCH_REQUIRED,
            TenantSettingKeys.ATTENDANCE_DAILY_PROCESSING_ENABLED,
            TenantSettingKeys.ATTENDANCE_ADJUSTMENT_APPROVAL_REQUIRED,
            TenantSettingKeys.LEAVE_SELF_SERVICE_ENABLED,
            TenantSettingKeys.LEAVE_SUPERVISOR_APPROVAL_ENABLED);
    private final TenantSettingRepository repository;
    public TenantSettingsValidationService(TenantSettingRepository repository) { this.repository = repository; }
    public String normalizeCountry(String value) {
        String v = normalize(value); if (v == null) return null; v = v.toUpperCase(Locale.ROOT);
        if (v.length() != 2 || !ISO_COUNTRIES.contains(v)) throw new BusinessException("El país no es válido"); return v;
    }
    public String normalizeCurrency(String value) {
        String v = normalize(value); if (v == null) throw new BusinessException("La moneda es obligatoria"); v = v.toUpperCase(Locale.ROOT);
        try { Currency.getInstance(v); } catch (IllegalArgumentException ex) { throw new BusinessException("La moneda no es válida"); } return v;
    }
    public String normalizeTimezone(String value) {
        String v = normalize(value);
        if (v == null || !ZoneId.getAvailableZoneIds().contains(v) || ZoneId.of(v).getRules().isFixedOffset()) throw new BusinessException("La zona horaria no es válida");
        return v;
    }
    public void validateRequiredCompanySettings() { if (!missingCompanyFields().isEmpty()) throw new InvalidBusinessStateException("La configuración empresarial contiene valores inválidos"); }
    public void validateHrPolicies() { if (!missingPolicyFields().isEmpty()) throw new InvalidBusinessStateException("La configuración de RR. HH. contiene valores inválidos"); }
    public List<String> missingFields() { var result = new ArrayList<String>(); result.addAll(missingCompanyFields()); result.addAll(missingPolicyFields()); return List.copyOf(result); }
    private List<String> missingCompanyFields() {
        Map<String,String> v = values(); var r = new ArrayList<String>(); String name = v.get(TenantSettingKeys.COMPANY_NAME);
        if (name == null || name.trim().isEmpty() || name.trim().length() > 150) r.add("company.name");
        if (!isCountry(v.get(TenantSettingKeys.COMPANY_COUNTRY))) r.add("company.country");
        if (!isCurrency(v.get(TenantSettingKeys.COMPANY_CURRENCY))) r.add("company.currency");
        if (!isTimezone(v.get(TenantSettingKeys.COMPANY_TIMEZONE))) r.add("company.timezone"); return r;
    }
    private List<String> missingPolicyFields() {
        Map<String,String> v = values(); var r = new ArrayList<String>();
        for (String key : POLICY_KEYS) { String value=v.get(key); if (!isBoolean(value) || (TenantSettingKeys.ATTENDANCE_ADJUSTMENT_APPROVAL_REQUIRED.equals(key) && !"true".equalsIgnoreCase(value == null ? null : value.trim()))) r.add(key); }
        return r;
    }
    private Map<String,String> values() { Map<String,String> v=new HashMap<>(); for (TenantSetting s:repository.findAll()) v.put(s.settingKey(),s.settingValue()); return v; }
    private boolean isCountry(String v) { return v != null && v.trim().equals(v) && v.length()==2 && ISO_COUNTRIES.contains(v.toUpperCase(Locale.ROOT)); }
    private boolean isCurrency(String v) { if(v==null||!v.trim().equals(v)||v.length()!=3)return false; try{Currency.getInstance(v.toUpperCase(Locale.ROOT));return true;}catch(IllegalArgumentException e){return false;} }
    private boolean isTimezone(String v) { if(v==null||!v.trim().equals(v)||!ZoneId.getAvailableZoneIds().contains(v))return false; return !ZoneId.of(v).getRules().isFixedOffset(); }
    private boolean isBoolean(String v) { return v != null && ("true".equalsIgnoreCase(v.trim()) || "false".equalsIgnoreCase(v.trim())); }
    private String normalize(String v) { if(v==null)return null; String n=v.trim(); return n.isEmpty()?null:n; }
}
