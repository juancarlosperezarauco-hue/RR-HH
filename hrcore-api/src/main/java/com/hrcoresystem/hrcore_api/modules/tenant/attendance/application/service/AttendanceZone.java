package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class AttendanceZone {
    private final TenantTimezoneService timezoneService;
    public AttendanceZone(TenantTimezoneService timezoneService){this.timezoneService=timezoneService;}
    public ZoneId required(){return timezoneService.requiredZone();}
    public Instant uniqueInstant(LocalDateTime local,ZoneId zone){var offsets=zone.getRules().getValidOffsets(local);if(offsets.size()!=1)throw new IndeterminateScheduleTime(local);return local.atOffset(offsets.getFirst()).toInstant();}
    public static final class IndeterminateScheduleTime extends RuntimeException{public IndeterminateScheduleTime(LocalDateTime local){super("Ambiguous or nonexistent local time: "+local);}}
}
