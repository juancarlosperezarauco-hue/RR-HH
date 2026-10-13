package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.CreateShiftAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.UpdateShiftAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.EmployeeShiftAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmployeeShiftAssignmentServiceTest {
    private final ShiftAssignmentRepository assignments = mock(ShiftAssignmentRepository.class);
    private final WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final TenantShiftClock clock = mock(TenantShiftClock.class);
    private final SecurityContextFacade security = mock(SecurityContextFacade.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final EmployeeShiftAssignmentService service = new EmployeeShiftAssignmentService(assignments,
            shifts, employees, plans, clock, security, audit, new ShiftAssignmentConflictPolicy());
    private final LocalDate today = LocalDate.of(2026, 10, 13);
    private final UUID employeeId = UUID.randomUUID();
    private final UUID shiftId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(clock.today()).thenReturn(today);
        when(employees.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee()));
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee()));
        when(shifts.lockById(shiftId)).thenReturn(Optional.of(shift(shiftId)));
        when(security.getCurrentSubject()).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    void createSortsWeekdaysAndAuditsOnce() {
        when(assignments.save(any())).thenAnswer(invocation -> {
            EmployeeShiftAssignment draft = invocation.getArgument(0);
            return new EmployeeShiftAssignment(UUID.randomUUID(), draft.employeeId(), draft.shiftId(),
                    draft.startDate(), draft.endDate(), draft.weekdays(), draft.status(),
                    draft.createdBy(), null, null);
        });
        var response = service.create(new CreateShiftAssignmentRequest(employeeId, shiftId,
                today, null, List.of((short) 7, (short) 1)));
        assertEquals(List.of((short) 1, (short) 7), response.weekdays());
        assertEquals(ShiftAssignmentStatus.ACTIVE, response.status());
        verify(assignments).save(any());
        verify(audit).recordTenantEvent(eq("SHIFT_ASSIGNMENT_CREATED"), eq("EMPLOYEE_SHIFT_ASSIGNMENT"),
                any(), any());
    }

    @Test
    void duplicateWeekdaysAreRejectedBeforeWriting() {
        assertThrows(BusinessException.class, () -> service.create(new CreateShiftAssignmentRequest(
                employeeId, shiftId, today, null, List.of((short) 2, (short) 2))));
        verify(assignments, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void sameFutureUpdateDoesNotWriteOrAudit() {
        UUID id = UUID.randomUUID();
        LocalDate start = today.plusDays(2);
        EmployeeShiftAssignment before = assignment(id, start, null, List.of((short) 1),
                ShiftAssignmentStatus.ACTIVE);
        when(assignments.findEmployeeId(id)).thenReturn(Optional.of(employeeId));
        when(assignments.findById(id)).thenReturn(Optional.of(before));
        service.update(id, new UpdateShiftAssignmentRequest(shiftId, start, null, List.of((short) 1)));
        verify(assignments, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void closePreservesEarlierEndDateAndNoOpsWhenAlreadyClosed() {
        UUID id = UUID.randomUUID();
        LocalDate priorEnd = today.minusDays(2);
        EmployeeShiftAssignment before = assignment(id, today.minusDays(5), priorEnd,
                List.of((short) 1), ShiftAssignmentStatus.ACTIVE);
        when(assignments.findEmployeeId(id)).thenReturn(Optional.of(employeeId));
        when(assignments.findById(id)).thenReturn(Optional.of(before));
        when(assignments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.close(id);
        assertEquals(priorEnd, response.endDate());
        assertEquals(ShiftAssignmentStatus.INACTIVE, response.status());
        verify(audit).recordTenantEvent(eq("SHIFT_ASSIGNMENT_CLOSED"), any(), any(), any());

        clearInvocations(assignments, audit);
        when(assignments.findEmployeeId(id)).thenReturn(Optional.of(employeeId));
        when(assignments.findById(id)).thenReturn(Optional.of(assignment(id, today.minusDays(5), priorEnd,
                List.of((short) 1), ShiftAssignmentStatus.INACTIVE)));
        service.close(id);
        verify(assignments, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void cancelPreservesFutureRowWithoutDeletion() {
        UUID id = UUID.randomUUID();
        when(assignments.findEmployeeId(id)).thenReturn(Optional.of(employeeId));
        when(assignments.findById(id)).thenReturn(Optional.of(assignment(id, today.plusDays(1), null,
                List.of((short) 3), ShiftAssignmentStatus.ACTIVE)));
        when(assignments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(ShiftAssignmentStatus.CANCELLED, service.cancel(id).status());
        verify(assignments).save(any());
        verify(audit).recordTenantEvent(eq("SHIFT_ASSIGNMENT_CANCELLED"), any(), any(), any());
    }

    @Test
    void rejectedOverlapHasNoAudit() {
        UUID otherShiftId = UUID.randomUUID();
        EmployeeShiftAssignment existing = new EmployeeShiftAssignment(UUID.randomUUID(), employeeId,
                otherShiftId, today, null, List.of((short) today.getDayOfWeek().getValue()),
                ShiftAssignmentStatus.ACTIVE, null, null, null);
        when(assignments.findActiveOverlapping(eq(employeeId), eq(null), any(), eq(null)))
                .thenReturn(List.of(existing));
        when(shifts.lockById(otherShiftId)).thenReturn(Optional.of(shift(otherShiftId)));
        assertThrows(BusinessException.class, () -> service.create(new CreateShiftAssignmentRequest(employeeId,
                shiftId, today, null, List.of((short) today.getDayOfWeek().getValue()))));
        verify(assignments, never()).save(any());
        verifyNoInteractions(audit);
    }

    private Employee employee() {
        return new Employee(employeeId, null, "E-1", "Ana", null, "Rojas", null, "CI", "123",
                null, null, null, null, null, null, null, null, LocalDate.of(2020, 1, 1), null,
                EmploymentStatus.ACTIVE, null, null, null, null);
    }

    private WorkShift shift(UUID id) {
        return new WorkShift(id, "DAY", "Day", null, LocalTime.of(8, 0), LocalTime.of(16, 0),
                false, 0, 0, ShiftStatus.ACTIVE, null, null, null);
    }

    private EmployeeShiftAssignment assignment(UUID id, LocalDate start, LocalDate end,
            List<Short> weekdays, ShiftAssignmentStatus status) {
        return new EmployeeShiftAssignment(id, employeeId, shiftId, start, end,
                weekdays, status, null, null, null);
    }
}
