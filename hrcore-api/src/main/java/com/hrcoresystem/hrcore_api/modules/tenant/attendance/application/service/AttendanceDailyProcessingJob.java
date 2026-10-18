package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContext;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceDailyProcessingJob {
    private static final Logger log = LoggerFactory.getLogger(AttendanceDailyProcessingJob.class);
    private final PlatformTenantRepository tenants;
    private final AttendanceDailyProcessingExecutor executor;
    private final TenantTimezoneService timezone;

    public AttendanceDailyProcessingJob(PlatformTenantRepository tenants, AttendanceDailyProcessingExecutor executor,
            TenantTimezoneService timezone) {
        this.tenants = tenants; this.executor = executor; this.timezone = timezone;
    }

    @Scheduled(fixedDelayString = "${hrcore.attendance.processing-ms:3600000}")
    public void processTenants() {
        for (PlatformTenant tenant : tenants.findAll()) {
            if (!tenant.active() || tenant.status() != PlatformTenantStatus.ACTIVE
                    || tenant.schemaName() == null || tenant.schemaName().isBlank()) continue;
            try {
                TenantContextHolder.set(new TenantContext(tenant.slug(), tenant.schemaName(), false));
                var result = executor.execute(timezone.today(), 7);
                log.info("Asistencia tenant={} candidates={} processed={} failed={}", tenant.slug(),
                        result.candidates(), result.processed(), result.failed());
            } catch (Exception ex) {
                log.error("Error procesando asistencia del tenant {}", tenant.slug(), ex);
            } finally { TenantContextHolder.clear(); }
        }
    }
}
