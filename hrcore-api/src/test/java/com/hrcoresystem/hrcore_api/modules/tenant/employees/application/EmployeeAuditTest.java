package com.hrcoresystem.hrcore_api.modules.tenant.employees.application;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeeAudit;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmployeeAuditTest {
    @Test void auditPayloadContainsFieldNamesWithoutPersonalValues() {
        AuditTrailService trail = mock(AuditTrailService.class);
        UUID id = UUID.randomUUID();
        new EmployeeAudit(trail).record("EMPLOYEE_CONTACT", "UPDATED", id, List.of("phone"));
        verify(trail).recordTenantEvent(eq("EMPLOYEE_CONTACT_UPDATED"), eq("EMPLOYEE_CONTACT"),
                eq(id.toString()), argThat(details -> {
                    assertInstanceOf(Map.class, details);
                    String rendered = details.toString();
                    return rendered.contains("phone") && !rendered.contains("59170000000");
                }));
    }
}
