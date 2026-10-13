package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftBreakRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AttendancePunchServiceTest {
    private final AttendanceStore store = mock(AttendanceStore.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final AttendanceCalculationService calculations = mock(AttendanceCalculationService.class);
    private final EffectiveScheduleService schedules = mock(EffectiveScheduleService.class);
    private final ShiftAssignmentRepository assignments = mock(ShiftAssignmentRepository.class);
    private final WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    private final ShiftBreakRepository breaks = mock(ShiftBreakRepository.class);
    private final AttendanceZone zones = mock(AttendanceZone.class);
    private final AttendanceActor actor = mock(AttendanceActor.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final UUID userId = UUID.randomUUID(), employeeId = UUID.randomUUID(), shiftId = UUID.randomUUID();
    private AttendancePunchService service;

    @BeforeEach
    void setUp() {
        service = new AttendancePunchService(store, employees, calculations, schedules, assignments, shifts, breaks,
                zones, actor, new AttendanceMapper(), plans, audit);
        when(actor.id()).thenReturn(userId);
        when(store.employeeForUser(userId)).thenReturn(Optional.of(employeeId));
        when(zones.required()).thenReturn(ZoneOffset.UTC);
        when(zones.uniqueInstant(any(LocalDateTime.class), eq(ZoneOffset.UTC)))
                .thenAnswer(i -> ((LocalDateTime) i.getArgument(0)).toInstant(ZoneOffset.UTC));
        when(employees.findByIdForUpdate(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(store.punches(eq(employeeId), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(store.save(any(AttendancePunch.class))).thenAnswer(i -> {
            AttendancePunch p = i.getArgument(0);
            return new AttendancePunch(UUID.randomUUID(), p.employeeId(), p.attendanceRecordId(),
                    p.punchType(), p.punchedAt(), p.source(), p.deviceIdentifier(), p.latitude(),
                    p.longitude(), p.recordedBy(), p.notes(), Instant.now());
        });
    }

    @Test
    void ownPunchUsesServerTimeActorAndWebSource() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        exact(date);
        noWork(date.minusDays(1));
        AttendanceRecord record = record(date);
        when(store.lockOrCreate(employeeId, date)).thenReturn(record);
        when(actor.has("attendance.punches.create")).thenReturn(true);
        Instant before = Instant.now();
        var result = service.create(new AttendanceRequests.Punch(null, PunchType.IN, null,
                null, null, null));
        assertEquals(employeeId, result.employeeId());
        assertEquals(userId, result.recordedBy());
        assertEquals(PunchSource.WEB, result.source());
        assertEquals(record.id(), result.attendanceRecordId());
        assertFalse(result.punchedAt().isBefore(before));
        assertFalse(result.punchedAt().isAfter(Instant.now()));
        verify(calculations).recalculateLocked(record);
    }

    @Test
    void historicalPunchRequiresAdministrativePermissionAndExplanation() {
        Instant historical = Instant.parse("2026-01-12T08:00:00Z");
        var request = new AttendanceRequests.Punch(employeeId, PunchType.IN, historical,
                null, null, "historical entry");
        assertThrows(AccessDeniedException.class, () -> service.create(request));
        verify(store, never()).save(any(AttendancePunch.class));
        when(actor.has("attendance.punches.manage")).thenReturn(true);
        assertThrows(com.hrcoresystem.hrcore_api.common.exception.BusinessException.class,
                () -> service.create(new AttendanceRequests.Punch(employeeId, PunchType.IN, historical,
                        null, null, null)));
        exact(LocalDate.of(2026, 1, 12));
        noWork(LocalDate.of(2026, 1, 11));
        var result = service.create(request);
        assertEquals(PunchSource.MANUAL, result.source());
        assertEquals(userId, result.recordedBy());
    }

    @Test
    void ambiguousAssignmentKeepsPunchUnassociated() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        exact(date);
        noWork(date.minusDays(1));
        when(assignments.findEffectiveOn(employeeId, date)).thenReturn(List.of(
                assignment(date), assignment(date)));
        when(actor.has("attendance.punches.create")).thenReturn(true);
        var result = service.create(new AttendanceRequests.Punch(null, PunchType.IN, null,
                null, null, null));
        assertNull(result.attendanceRecordId());
        verify(store, never()).lockOrCreate(any(UUID.class), any(LocalDate.class));
        verifyNoInteractions(calculations);
    }

    @Test
    void unresolvedPreviousNightScheduleKeepsPunchUnassociated() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        exact(date);
        LocalDate prior = date.minusDays(1);
        when(schedules.get(employeeId, prior)).thenReturn(new ScheduleResponse(employeeId, prior,
                null, UUID.randomUUID(), "EMPLOYEE_EXCEPTION", ScheduleDetermination.INDETERMINATE,
                "SCHEDULE_CHANGE_WITHOUT_HOURS", List.of()));
        when(actor.has("attendance.punches.create")).thenReturn(true);
        var result = service.create(new AttendanceRequests.Punch(null, PunchType.IN, null,
                null, null, null));
        assertNull(result.attendanceRecordId());
    }

    private void exact(LocalDate date) {
        when(schedules.get(employeeId, date)).thenReturn(new ScheduleResponse(employeeId, date, null, null,
                "USUAL_ASSIGNMENT", ScheduleDetermination.EXACT, null,
                List.of(new ScheduleInterval(shiftId, date.atTime(8, 0), date.atTime(16, 0)))));
        when(assignments.findEffectiveOn(employeeId, date)).thenReturn(List.of(assignment(date)));
        when(shifts.findById(shiftId)).thenReturn(Optional.of(new WorkShift(shiftId, "S", "Shift", null,
                LocalTime.of(8, 0), LocalTime.of(16, 0), false, 0, 0,
                ShiftStatus.ACTIVE, null, null, null)));
        when(breaks.findAllByShiftId(shiftId)).thenReturn(List.of());
    }
    private void noWork(LocalDate date) {
        when(schedules.get(employeeId, date)).thenReturn(new ScheduleResponse(employeeId, date, null, null,
                "USUAL_ASSIGNMENT", ScheduleDetermination.NO_WORK, null, List.of()));
    }
    private EmployeeShiftAssignment assignment(LocalDate date) {
        return new EmployeeShiftAssignment(UUID.randomUUID(), employeeId, shiftId, date, null,
                List.of((short) 1), ShiftAssignmentStatus.ACTIVE, null, null, null);
    }
    private AttendanceRecord record(LocalDate date) {
        return new AttendanceRecord(UUID.randomUUID(), employeeId, null, date,
                null, null, null, null, 0, 0, 0, AttendanceStatus.PENDING,
                null, null, Instant.now(), Instant.now());
    }
}
