package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.ApprovedLeaveLookup;
import java.math.BigDecimal;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.*;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttendanceCalculationServiceTest {
    private final UUID employeeId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final UUID shiftId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 1, 12);
    private final AttendanceStore store = mock(AttendanceStore.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EffectiveScheduleService schedules = mock(EffectiveScheduleService.class);
    private final ShiftAssignmentRepository assignments = mock(ShiftAssignmentRepository.class);
    private final WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    private final ShiftBreakRepository breaks = mock(ShiftBreakRepository.class);
    private final AttendanceZone zones = mock(AttendanceZone.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final ApprovedLeaveLookup approvedLeaves = mock(ApprovedLeaveLookup.class);
    private AttendanceCalculationService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceCalculationService(store, employees, schedules, assignments, shifts, breaks,
                zones, plans, audit, new AttendanceMapper(), new ObjectMapper(), approvedLeaves);
        when(zones.required()).thenReturn(ZoneOffset.UTC);
        when(zones.uniqueInstant(any(LocalDateTime.class), eq(ZoneOffset.UTC)))
                .thenAnswer(invocation -> ((LocalDateTime) invocation.getArgument(0)).toInstant(ZoneOffset.UTC));
        when(employees.findByIdForUpdate(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(store.lockOrCreate(employeeId, date)).thenReturn(initial());
        when(store.adjustments(recordId)).thenReturn(List.of());
        when(store.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvedLeaves.onDate(employeeId, date)).thenReturn(List.of());
    }

    @Test
    void storesRealLateMinutesAndUsesToleranceOnlyForStatus() {
        exact(LocalTime.of(8, 0), LocalTime.of(16, 0), false, 10);
        when(store.punches(recordId)).thenReturn(List.of(
                punch(PunchType.IN, "2026-01-12T08:12:00Z"), punch(PunchType.OUT, "2026-01-12T16:00:00Z")));
        var result = service.calculate(employeeId, date);
        assertEquals(AttendanceStatus.LATE, result.status());
        assertEquals(12, result.lateMinutes());
        assertEquals(468, result.workedMinutes());

        when(store.punches(recordId)).thenReturn(List.of(
                punch(PunchType.IN, "2026-01-12T08:07:00Z"), punch(PunchType.OUT, "2026-01-12T16:00:00Z")));
        var withinTolerance = service.calculate(employeeId, date);
        assertEquals(AttendanceStatus.PRESENT, withinTolerance.status());
        assertEquals(7, withinTolerance.lateMinutes());
    }

    @Test
    void nightShiftUsesStartDateAndNextDayExit() {
        exact(LocalTime.of(22, 0), LocalTime.of(6, 0), true, 0);
        when(store.punches(recordId)).thenReturn(List.of(
                punch(PunchType.IN, "2026-01-12T22:00:00Z"), punch(PunchType.OUT, "2026-01-13T06:00:00Z")));
        var result = service.calculate(employeeId, date);
        assertEquals(date, result.workDate());
        assertEquals(Instant.parse("2026-01-13T06:00:00Z"), result.expectedEndAt());
        assertEquals(480, result.workedMinutes());
        assertEquals(AttendanceStatus.PRESENT, result.status());
    }

    @Test
    void scheduledDayWithoutPunchesBecomesAbsentAfterShiftEnd() {
        exact(LocalTime.of(8, 0), LocalTime.of(16, 0), false, 0);
        when(store.punches(recordId)).thenReturn(List.of());
        assertEquals(AttendanceStatus.ABSENT, service.calculate(employeeId, date).status());
    }

    @Test
    void approvedFullDayLeaveJustifiesTheScheduledAbsence() {
        exact(LocalTime.of(8, 0), LocalTime.of(16, 0), false, 0);
        when(store.punches(recordId)).thenReturn(List.of());
        UUID leaveRequestId = UUID.randomUUID();
        when(approvedLeaves.onDate(employeeId, date)).thenReturn(List.of(
                new ApprovedLeaveLookup.ApprovedPeriod(leaveRequestId, UUID.randomUUID(), date, date,
                        null, null, BigDecimal.ONE)));

        var result = service.calculate(employeeId, date);

        assertEquals(AttendanceStatus.JUSTIFIED, result.status());
        assertEquals("APPROVED_LEAVE", result.calculationDetails().path("reason").asText());
        assertEquals(leaveRequestId.toString(),
                result.calculationDetails().path("approvedLeaveRequestIds").get(0).asText());
    }

    @Test
    void incompletePunchPrecedesLateAndKeepsActualLateMinutes() {
        exact(LocalTime.of(8, 0), LocalTime.of(16, 0), false, 10);
        when(store.punches(recordId)).thenReturn(List.of(punch(PunchType.IN, "2026-01-12T08:12:00Z")));
        var result = service.calculate(employeeId, date);
        assertEquals(AttendanceStatus.INCOMPLETE, result.status());
        assertEquals(12, result.lateMinutes());
        assertEquals("OPEN_IN", result.calculationDetails().path("reason").asText());
    }

    @Test
    void noScheduleStaysPendingButExplicitDayOffIsRecognized() {
        when(store.punches(recordId)).thenReturn(List.of());
        when(schedules.get(employeeId, date)).thenReturn(new ScheduleResponse(employeeId, date, null, null,
                "USUAL_ASSIGNMENT", ScheduleDetermination.NO_WORK, null, List.of()));
        assertEquals(AttendanceStatus.PENDING, service.calculate(employeeId, date).status());
        when(schedules.get(employeeId, date)).thenReturn(new ScheduleResponse(employeeId, date, null, UUID.randomUUID(),
                "ORGANIZATION_EXCEPTION", ScheduleDetermination.NO_WORK, null, List.of()));
        assertEquals(AttendanceStatus.DAY_OFF, service.calculate(employeeId, date).status());
    }

    @Test
    void multipleAssignmentsAndScheduleChangeAreIncomplete() {
        exact(LocalTime.of(8, 0), LocalTime.of(16, 0), false, 0);
        var one = assignment();
        when(assignments.findEffectiveOn(employeeId, date)).thenReturn(List.of(one, assignment()));
        when(store.punches(recordId)).thenReturn(List.of());
        assertEquals(AttendanceStatus.INCOMPLETE, service.calculate(employeeId, date).status());
        when(schedules.get(employeeId, date)).thenReturn(new ScheduleResponse(employeeId, date, null,
                UUID.randomUUID(), "EMPLOYEE_EXCEPTION", ScheduleDetermination.INDETERMINATE,
                "SCHEDULE_CHANGE_WITHOUT_HOURS", List.of()));
        var unresolved = service.calculate(employeeId, date);
        assertEquals(AttendanceStatus.INCOMPLETE, unresolved.status());
        assertEquals("SCHEDULE_CHANGE_WITHOUT_HOURS", unresolved.calculationDetails().path("reason").asText());
    }

    private void exact(LocalTime start, LocalTime end, boolean night, int lateTolerance) {
        when(schedules.get(employeeId, date)).thenReturn(new ScheduleResponse(employeeId, date, null, null,
                "USUAL_ASSIGNMENT", ScheduleDetermination.EXACT, null,
                List.of(new ScheduleInterval(shiftId, date.atTime(start), date.plusDays(night ? 1 : 0).atTime(end)))));
        when(assignments.findEffectiveOn(employeeId, date)).thenReturn(List.of(assignment()));
        when(shifts.findById(shiftId)).thenReturn(Optional.of(new WorkShift(shiftId, "S", "Shift", null,
                start, end, night, lateTolerance, 0, ShiftStatus.ACTIVE, null, null, null)));
        when(breaks.findAllByShiftId(shiftId)).thenReturn(List.of());
    }

    private EmployeeShiftAssignment assignment() {
        return new EmployeeShiftAssignment(assignmentId, employeeId, shiftId, date, null, List.of((short) 1),
                ShiftAssignmentStatus.ACTIVE, null, null, null);
    }

    private AttendanceRecord initial() {
        return new AttendanceRecord(recordId, employeeId, null, date, null, null, null, null,
                0, 0, 0, AttendanceStatus.PENDING, null, null, Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    private AttendancePunch punch(PunchType type, String at) {
        return new AttendancePunch(UUID.randomUUID(), employeeId, recordId, type, Instant.parse(at), PunchSource.WEB,
                null, null, null, UUID.randomUUID(), null, null);
    }
}
