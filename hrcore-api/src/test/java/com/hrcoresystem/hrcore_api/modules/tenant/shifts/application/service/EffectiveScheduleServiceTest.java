package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleDetermination;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EffectiveScheduleServiceTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EmployeeDepartmentAtDateRepository placements = mock(EmployeeDepartmentAtDateRepository.class);
    private final WorkCalendarExceptionRepository exceptions = mock(WorkCalendarExceptionRepository.class);
    private final ShiftAssignmentRepository assignments = mock(ShiftAssignmentRepository.class);
    private final WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final EffectiveScheduleService service = new EffectiveScheduleService(
            employees, placements, exceptions, assignments, shifts, plans);
    private final UUID employeeId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 10, 14);

    @BeforeEach
    void setUp() {
        when(employees.findById(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
    }

    @Test
    void employeeExceptionHasPriorityOverAmbiguousDepartment() {
        when(exceptions.findByScopeAndDate(employeeId, null, date))
                .thenReturn(List.of(exception(CalendarExceptionType.HOLIDAY, false)));
        var result = service.get(employeeId, date);
        assertEquals(ScheduleDetermination.NO_WORK, result.determination());
        assertEquals("EMPLOYEE_EXCEPTION", result.source());
        verifyNoInteractions(placements, assignments, shifts);
    }

    @Test
    void ambiguousDepartmentAndInheritedDuplicatesAreExplicit() {
        when(placements.findAtDate(employeeId, date)).thenReturn(List.of(
                new EmployeeDepartmentAtDateRepository.Placement(UUID.randomUUID(), false),
                new EmployeeDepartmentAtDateRepository.Placement(UUID.randomUUID(), false)));
        var ambiguous = service.get(employeeId, date);
        assertEquals(ScheduleDetermination.INDETERMINATE, ambiguous.determination());
        assertEquals("AMBIGUOUS_DEPARTMENT", ambiguous.reason());

        reset(placements);
        when(exceptions.findByScopeAndDate(null, null, date)).thenReturn(List.of(
                exception(CalendarExceptionType.OTHER, false),
                exception(CalendarExceptionType.OTHER, false)));
        var duplicate = service.get(employeeId, date);
        assertEquals(ScheduleDetermination.INDETERMINATE, duplicate.determination());
        assertEquals("DUPLICATE_EXCEPTION", duplicate.reason());
    }

    @Test
    void primaryDepartmentWinsOverAdditionalAssignment() {
        UUID primaryDepartment = UUID.randomUUID();
        when(placements.findAtDate(employeeId, date)).thenReturn(List.of(
                new EmployeeDepartmentAtDateRepository.Placement(UUID.randomUUID(), false),
                new EmployeeDepartmentAtDateRepository.Placement(primaryDepartment, true)));
        when(exceptions.findByScopeAndDate(null, primaryDepartment, date))
                .thenReturn(List.of(exception(CalendarExceptionType.NON_WORKING_DAY, false)));
        var result = service.get(employeeId, date);
        assertEquals(primaryDepartment, result.departmentId());
        assertEquals(ScheduleDetermination.NO_WORK, result.determination());
        assertEquals("DEPARTMENT_EXCEPTION", result.source());
    }

    @Test
    void overnightShiftProducesIntervalOnStartDate() {
        UUID shiftId = UUID.randomUUID();
        when(assignments.findEffectiveOn(employeeId, date)).thenReturn(List.of(assignment(shiftId)));
        when(shifts.findById(shiftId)).thenReturn(Optional.of(
                shift(shiftId, LocalTime.of(22, 0), LocalTime.of(6, 0), true)));
        var result = service.get(employeeId, date);
        assertEquals(ScheduleDetermination.EXACT, result.determination());
        assertEquals(date.atTime(22, 0), result.intervals().getFirst().start());
        assertEquals(date.plusDays(1).atTime(6, 0), result.intervals().getFirst().end());
    }

    @Test
    void scheduleChangeWithRequiredWorkDoesNotInventHours() {
        when(exceptions.findByScopeAndDate(employeeId, null, date))
                .thenReturn(List.of(exception(CalendarExceptionType.SCHEDULE_CHANGE, true)));
        var result = service.get(employeeId, date);
        assertEquals(ScheduleDetermination.INDETERMINATE, result.determination());
        assertEquals("SCHEDULE_CHANGE_WITHOUT_HOURS", result.reason());
        assertTrue(result.intervals().isEmpty());
    }

    @Test
    void overlappingUsualShiftsAreIndeterminateButTouchingShiftsAreExact() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(assignments.findEffectiveOn(employeeId, date)).thenReturn(List.of(assignment(first), assignment(second)));
        when(shifts.findById(first)).thenReturn(Optional.of(shift(first,
                LocalTime.of(8, 0), LocalTime.of(12, 0), false)));
        when(shifts.findById(second)).thenReturn(Optional.of(shift(second,
                LocalTime.of(11, 0), LocalTime.of(17, 0), false)));
        assertEquals(ScheduleDetermination.INDETERMINATE, service.get(employeeId, date).determination());
        when(shifts.findById(second)).thenReturn(Optional.of(shift(second,
                LocalTime.of(12, 0), LocalTime.of(17, 0), false)));
        assertEquals(ScheduleDetermination.EXACT, service.get(employeeId, date).determination());
    }

    @Test
    void priorNightShiftCanConflictButIsNotReturnedAsTodaysInterval() {
        UUID priorId = UUID.randomUUID();
        UUID todayId = UUID.randomUUID();
        when(assignments.findEffectiveOn(employeeId, date.minusDays(1)))
                .thenReturn(List.of(assignment(priorId, date.minusDays(1))));
        when(assignments.findEffectiveOn(employeeId, date))
                .thenReturn(List.of(assignment(todayId)));
        when(shifts.findById(priorId)).thenReturn(Optional.of(shift(priorId,
                LocalTime.of(22, 0), LocalTime.of(6, 0), true)));
        when(shifts.findById(todayId)).thenReturn(Optional.of(shift(todayId,
                LocalTime.of(5, 0), LocalTime.of(12, 0), false)));
        assertEquals(ScheduleDetermination.INDETERMINATE, service.get(employeeId, date).determination());

        when(shifts.findById(todayId)).thenReturn(Optional.of(shift(todayId,
                LocalTime.of(6, 0), LocalTime.of(12, 0), false)));
        var touching = service.get(employeeId, date);
        assertEquals(ScheduleDetermination.EXACT, touching.determination());
        assertEquals(1, touching.intervals().size());
        assertEquals(todayId, touching.intervals().getFirst().shiftId());
    }

    private WorkCalendarException exception(CalendarExceptionType type, boolean required) {
        return new WorkCalendarException(UUID.randomUUID(), employeeId, null, date,
                type, null, required, null, null);
    }

    private EmployeeShiftAssignment assignment(UUID shiftId) {
        return assignment(shiftId, date);
    }

    private EmployeeShiftAssignment assignment(UUID shiftId, LocalDate startDate) {
        return new EmployeeShiftAssignment(UUID.randomUUID(), employeeId, shiftId, startDate, null,
                List.of((short) startDate.getDayOfWeek().getValue()), ShiftAssignmentStatus.ACTIVE,
                null, null, null);
    }

    private WorkShift shift(UUID id, LocalTime start, LocalTime end, boolean crosses) {
        return new WorkShift(id, "S", "Shift", null, start, end, crosses, 0, 0,
                ShiftStatus.ACTIVE, null, null, null);
    }
}
