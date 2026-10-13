package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.EmployeeShiftAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftAssignmentConflictPolicyTest {
    private final ShiftAssignmentConflictPolicy policy = new ShiftAssignmentConflictPolicy();

    @Test
    void overlappingHoursOnSameWeekdayConflict() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        assertTrue(policy.conflicts(assignment(monday, null, List.of((short) 1)),
                shift("08:00", "12:00", false),
                assignment(monday, null, List.of((short) 1)),
                shift("11:59", "17:00", false)));
    }

    @Test
    void touchingHalfOpenIntervalsAreCompatible() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        assertFalse(policy.conflicts(assignment(monday, null, List.of((short) 1)),
                shift("08:00", "12:00", false),
                assignment(monday, null, List.of((short) 1)),
                shift("12:00", "16:00", false)));
    }

    @Test
    void overnightOccurrenceConflictsWithNextDayIncludingLastValidDay() {
        LocalDate sunday = LocalDate.of(2026, 1, 4);
        LocalDate monday = sunday.plusDays(1);
        assertTrue(policy.conflicts(assignment(monday, monday, List.of((short) 1)),
                shift("01:00", "05:00", false),
                assignment(sunday, sunday, List.of((short) 7)),
                shift("22:00", "03:00", true)));
    }

    @Test
    void dateBoundsPreventFalseWeeklyConflict() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        assertFalse(policy.conflicts(assignment(monday.plusWeeks(1), monday.plusWeeks(1), List.of((short) 1)),
                shift("08:00", "12:00", false),
                assignment(monday, monday, List.of((short) 1)),
                shift("08:00", "12:00", false)));
    }

    @Test
    void fullDayShiftConflictsUntilNextDayStartAndThenOnlyTouches() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        EmployeeShiftAssignment first = assignment(monday, monday, List.of((short) 1));
        WorkShift fullDay = shift("08:00", "08:00", true);
        assertTrue(policy.conflicts(first, fullDay,
                assignment(monday.plusDays(1), monday.plusDays(1), List.of((short) 2)),
                shift("07:59", "09:00", false)));
        assertFalse(policy.conflicts(first, fullDay,
                assignment(monday.plusDays(1), monday.plusDays(1), List.of((short) 2)),
                shift("08:00", "09:00", false)));
    }

    private EmployeeShiftAssignment assignment(LocalDate start, LocalDate end, List<Short> weekdays) {
        return new EmployeeShiftAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                start, end, weekdays, ShiftAssignmentStatus.ACTIVE, null, null, null);
    }

    private WorkShift shift(String start, String end, boolean overnight) {
        return new WorkShift(UUID.randomUUID(), "CODE", "Name", null, LocalTime.parse(start), LocalTime.parse(end),
                overnight, 0, 0, ShiftStatus.ACTIVE, null, null, null);
    }
}
