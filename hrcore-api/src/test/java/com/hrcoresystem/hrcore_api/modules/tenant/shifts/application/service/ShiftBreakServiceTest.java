package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftBreakRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftBreak;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftBreakRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
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

class ShiftBreakServiceTest {
    private final WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    private final ShiftBreakRepository breaks = mock(ShiftBreakRepository.class);
    private final ShiftAssignmentRepository assignments = mock(ShiftAssignmentRepository.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final ShiftBreakService service = new ShiftBreakService(shifts, breaks, assignments, plans, audit);

    @Test
    void overlappingBreaksAreRejected() {
        WorkShift shift = shift();
        when(shifts.lockById(shift.id())).thenReturn(Optional.of(shift));
        when(breaks.findAllByShiftId(shift.id())).thenReturn(List.of(
                shiftBreak(shift.id(), "23:30", "00:00", 30)));
        assertThrows(BusinessException.class,
                () -> service.create(shift.id(), request("23:45", "00:15", 30)));
        verify(breaks, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void adjacentBreakIsAllowedAndAudited() {
        WorkShift shift = shift();
        ShiftBreak saved = shiftBreak(shift.id(), "00:00", "00:30", 30);
        when(shifts.lockById(shift.id())).thenReturn(Optional.of(shift));
        when(breaks.findAllByShiftId(shift.id())).thenReturn(List.of(
                shiftBreak(shift.id(), "23:30", "00:00", 30)));
        when(breaks.save(any())).thenReturn(saved);
        assertEquals(saved.id(), service.create(shift.id(), request("00:00", "00:30", 30)).id());
        verify(audit).recordTenantEvent(eq("SHIFT_BREAK_CREATED"), eq("SHIFT_BREAK"),
                eq(saved.id().toString()), any(), eq(null), eq(saved));
    }

    @Test
    void historicalShiftAllowsNameOnlyAndNoOpSkipsAudit() {
        WorkShift shift = shift();
        ShiftBreak before = shiftBreak(shift.id(), "23:30", "00:00", 30);
        when(shifts.lockById(shift.id())).thenReturn(Optional.of(shift));
        when(breaks.findByIdAndShiftId(before.id(), shift.id())).thenReturn(Optional.of(before));
        service.update(shift.id(), before.id(), request("23:30", "00:00", 30));
        verify(breaks, never()).save(any());
        verifyNoInteractions(audit);
        when(assignments.existsAnyByShiftId(shift.id())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.update(shift.id(), before.id(),
                request("23:45", "00:15", 30)));
    }

    @Test
    void historicalShiftCannotRemoveBreak() {
        WorkShift shift = shift();
        ShiftBreak before = shiftBreak(shift.id(), "23:30", "00:00", 30);
        when(shifts.lockById(shift.id())).thenReturn(Optional.of(shift));
        when(breaks.findByIdAndShiftId(before.id(), shift.id())).thenReturn(Optional.of(before));
        when(assignments.existsAnyByShiftId(shift.id())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.delete(shift.id(), before.id()));
        verify(breaks, never()).delete(any());
    }

    private WorkShift shift() {
        return new WorkShift(UUID.randomUUID(), "N1", "Night", null,
                LocalTime.of(22, 0), LocalTime.of(6, 0), true,
                0, 0, ShiftStatus.ACTIVE, null, null, null);
    }

    private ShiftBreak shiftBreak(UUID shiftId, String start, String end, int duration) {
        return new ShiftBreak(UUID.randomUUID(), shiftId, "Rest", LocalTime.parse(start),
                LocalTime.parse(end), duration, false, null);
    }

    private ShiftBreakRequest request(String start, String end, int duration) {
        return new ShiftBreakRequest("Rest", LocalTime.parse(start), LocalTime.parse(end), duration, false);
    }
}
