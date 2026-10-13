package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EmployeeAudit {
    private final AuditTrailService audit;
    public EmployeeAudit(AuditTrailService audit) { this.audit = audit; }
    public void record(String resource, String action, UUID id, List<String> changedFields) {
        audit.recordTenantEvent(resource + "_" + action, resource, id.toString(),
                Map.of("action", action, "changedFields", changedFields));
    }
}
