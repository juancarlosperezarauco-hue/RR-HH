package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service;

import com.hrcoresystem.hrcore_api.common.tenancy.context.*;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import java.time.LocalDate;
import org.slf4j.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmployeeDocumentExpirationScheduler {
  private static final Logger log = LoggerFactory.getLogger(EmployeeDocumentExpirationScheduler.class);
  private final PlatformTenantRepository tenants; private final EmployeeDocumentRepository documents;
  private final EmployeeFileRepository files; private final EmployeeRepository employees;
  private final NotificationPublisherPort notifications; private final AuditTrailService audit;
  public EmployeeDocumentExpirationScheduler(PlatformTenantRepository tenants, EmployeeDocumentRepository documents,
      EmployeeFileRepository files, EmployeeRepository employees, NotificationPublisherPort notifications, AuditTrailService audit) {
    this.tenants=tenants; this.documents=documents; this.files=files; this.employees=employees; this.notifications=notifications; this.audit=audit;
  }
  @Scheduled(fixedDelayString="${hrcore.documents.expiration-ms:3600000}")
  public void notifyExpiring() {
    LocalDate today=LocalDate.now();
    for (PlatformTenant tenant: tenants.findAll()) {
      if (!tenant.active() || tenant.status()!=PlatformTenantStatus.ACTIVE || tenant.schemaName()==null) continue;
      try {
        TenantContextHolder.set(new TenantContext(tenant.slug(),tenant.schemaName(),false));
        for (var d: documents.findExpiring(today.plusDays(30), PageRequest.of(0,500))) {
          var file=files.findById(d.employeeFileId()).orElse(null); if (file==null) continue;
          var employee=employees.findById(file.employeeId()).orElse(null); if (employee==null || employee.tenantUserId()==null) continue;
          boolean expired=d.expiresAt().isBefore(today); String title=expired?"Documento vencido":"Documento próximo a vencer";
          String body=expired?"El documento '"+d.title()+"' ha vencido.":"El documento '"+d.title()+"' vence el "+d.expiresAt()+".";
          String event = expired ? "EXPIRED" : "EXPIRING";
          try { notifications.publish(new NotificationPublishRequest(employee.tenantUserId(), expired?NotificationType.CONTRACT_EXPIRED:NotificationType.CONTRACT_EXPIRING,
              NotificationCategory.SYSTEM, expired?NotificationPriority.HIGH:NotificationPriority.NORMAL,title,body,null,null,"/dashboard/files?documentId="+d.id()+"&event="+event,null));
            audit.recordTenantEvent("EMPLOYEE_DOCUMENT_EXPIRATION_NOTIFIED","EMPLOYEE_DOCUMENT",d.id().toString(),java.util.Map.of("expiresAt",d.expiresAt()));
          } catch(Exception ignored) { }
        }
      } catch(Exception ex) { log.warn("Error notificando vencimiento documental del tenant {}",tenant.slug(),ex); }
      finally { TenantContextHolder.clear(); }
    }
  }
}
