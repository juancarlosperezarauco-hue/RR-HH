package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class AttendancePolicyService {
    private final TenantSettingRepository settings;
    public AttendancePolicyService(TenantSettingRepository settings) { this.settings = settings; }
    public boolean enabled(String key, boolean fallback) {
        return settings.findByKey(key).map(v -> Boolean.parseBoolean(v.settingValue())).orElse(fallback);
    }
    public void validateWebPunch(java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        if (!enabled("attendance.employee_web_punch_enabled", true))
            throw new BusinessException("Las marcaciones web están deshabilitadas para este tenant");
        boolean required = enabled("attendance.geolocation_required", false);
        if (required && (latitude == null || longitude == null))
            throw new BusinessException("La geolocalización es obligatoria para registrar la marcación");
        if ((latitude == null) != (longitude == null))
            throw new BusinessException("La latitud y longitud deben enviarse juntas");
    }
}
