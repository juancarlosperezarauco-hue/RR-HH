package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class TenantShiftClock {
    private final TenantTimezoneService timezoneService;
    public TenantShiftClock(TenantTimezoneService timezoneService){this.timezoneService=timezoneService;}
    public LocalDate today(){return timezoneService.today();}
}
