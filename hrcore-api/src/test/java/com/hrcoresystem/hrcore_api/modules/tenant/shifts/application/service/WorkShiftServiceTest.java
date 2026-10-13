package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkShiftRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkShiftServiceTest {
    private final WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    private final ShiftAssignmentRepository assignments = mock(ShiftAssignmentRepository.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final TenantShiftClock clock = mock(TenantShiftClock.class);
    private final SecurityContextFacade security = mock(SecurityContextFacade.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final WorkShiftService service = new WorkShiftService(
            shifts, assignments, plans, clock, security, audit);

    @Test
    void duplicateCodePreventsCreation() {
        when(security.getCurrentSubject()).thenReturn(UUID.randomUUID().toString());
        when(shifts.existsByCodeOtherThan("N1", null)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(request("N1", "22:00")));
        verify(shifts, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void unchangedUpdateDoesNotWriteOrAudit() {
        WorkShift before = shift(ShiftStatus.ACTIVE);
        when(shifts.lockById(before.id())).thenReturn(Optional.of(before));
        assertEquals(before.id(), service.update(before.id(), request("N1", "22:00")).id());
        verify(shifts, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void assignmentHistoryBlocksScheduleChangeButAllowsMetadata() {
        WorkShift before = shift(ShiftStatus.ACTIVE);
        when(shifts.lockById(before.id())).thenReturn(Optional.of(before));
        when(assignments.existsAnyByShiftId(before.id())).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> service.update(before.id(), request("N1", "21:00")));
        verify(shifts, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void metadataUpdateWithHistoryWritesAndAudits() {
        WorkShift before = shift(ShiftStatus.ACTIVE);
        when(shifts.lockById(before.id())).thenReturn(Optional.of(before));
        when(shifts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        WorkShiftRequest renamed = new WorkShiftRequest("N1", "Late night", null,
                before.startTime(), before.endTime(), true, 5, 5);
        assertEquals("Late night", service.update(before.id(), renamed).name());
        verify(shifts).save(any(WorkShift.class));
        verify(audit).recordTenantEvent(eq("WORK_SHIFT_UPDATED"), eq("WORK_SHIFT"),
                eq(before.id().toString()), any(), eq(before), any(WorkShift.class));
    }

    @Test
    void currentOrFutureAssignmentsBlockDeactivation() {
        WorkShift before = shift(ShiftStatus.ACTIVE);
        LocalDate today = LocalDate.of(2026, 10, 13);
        when(shifts.lockById(before.id())).thenReturn(Optional.of(before));
        when(clock.today()).thenReturn(today);
        when(assignments.existsActiveCurrentOrFutureByShiftId(before.id(), today)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.deactivate(before.id()));
        verify(shifts, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void repeatedActivationDoesNotWriteOrAudit() {
        WorkShift before = shift(ShiftStatus.ACTIVE);
        when(shifts.lockById(before.id())).thenReturn(Optional.of(before));
        assertEquals(ShiftStatus.ACTIVE, service.activate(before.id()).status());
        verify(shifts, never()).save(any());
        verifyNoInteractions(audit);
    }

    private WorkShiftRequest request(String code, String start) {
        return new WorkShiftRequest(code, "Night", null,
                LocalTime.parse(start), LocalTime.of(6, 0), true, 5, 5);
    }

    private WorkShift shift(ShiftStatus status) {
        return new WorkShift(UUID.randomUUID(), "N1", "Night", null,
                LocalTime.of(22, 0), LocalTime.of(6, 0), true,
                5, 5, status, UUID.randomUUID(), null, null);
    }
}
