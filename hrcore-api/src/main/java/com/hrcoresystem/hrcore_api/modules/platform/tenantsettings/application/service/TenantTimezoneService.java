package com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import org.springframework.stereotype.Service; import java.time.*;
@Service public class TenantTimezoneService { private final TenantSettingRepository repo; public TenantTimezoneService(TenantSettingRepository r){repo=r;} public ZoneId requiredZone(){String v=repo.findByKey(TenantSettingKeys.COMPANY_TIMEZONE).map(s->s.settingValue()).map(String::trim).orElse(""); try{if(!ZoneId.getAvailableZoneIds().contains(v)||ZoneId.of(v).getRules().isFixedOffset())throw new IllegalArgumentException();return ZoneId.of(v);}catch(Exception e){throw new BusinessException("Configure una zona horaria IANA válida");}} public LocalDate today(){return LocalDate.now(requiredZone());} }
