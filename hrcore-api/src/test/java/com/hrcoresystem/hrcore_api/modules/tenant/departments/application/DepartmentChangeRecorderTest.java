package com.hrcoresystem.hrcore_api.modules.tenant.departments.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.DepartmentChangeRecorder;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DepartmentChangeRecorderTest {
    private final DepartmentHistoryRepository histories = mock(DepartmentHistoryRepository.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final DepartmentChangeRecorder recorder = new DepartmentChangeRecorder(histories, audit, new ObjectMapper());

    @Test
    void prioritizesReorganizationThenManagerChange() {
        Department before = department(null, null);
        Department bothChanged = department(UUID.randomUUID(), UUID.randomUUID());
        assertEquals(DepartmentHistoryAction.REORGANIZED, recorder.updateAction(before, bothChanged));
        assertEquals(DepartmentHistoryAction.MANAGER_CHANGED,
                recorder.updateAction(before, department(null, UUID.randomUUID())));
        assertFalse(recorder.changed(before, before));
    }

    @Test
    void savesSanitizedSnapshotsAndAuditsChange() {
        Department before = department(null, null);
        Department after = department(null, UUID.randomUUID());
        UUID actor = UUID.randomUUID();
        recorder.record(before, after, DepartmentHistoryAction.MANAGER_CHANGED, "New manager", actor);
        ArgumentCaptor<DepartmentHistory> captured = ArgumentCaptor.forClass(DepartmentHistory.class);
        verify(histories).save(captured.capture());
        DepartmentHistory event = captured.getValue();
        assertEquals(actor, event.performedBy());
        assertEquals("New manager", event.reason());
        assertNull(event.previousData().get("managerEmployeeId").textValue());
        assertEquals(after.managerEmployeeId().toString(), event.newData().get("managerEmployeeId").asText());
        assertFalse(event.newData().has("createdBy"));
        verify(audit).recordTenantEvent(eq("DEPARTMENT_MANAGER_CHANGED"), eq("DEPARTMENT"),
                eq(after.id().toString()), any());
    }

    private Department department(UUID parent, UUID manager) {
        return new Department(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "CODE", "Name", null, parent, manager, null, DepartmentStatus.ACTIVE,
                null, null, null, null);
    }
}
