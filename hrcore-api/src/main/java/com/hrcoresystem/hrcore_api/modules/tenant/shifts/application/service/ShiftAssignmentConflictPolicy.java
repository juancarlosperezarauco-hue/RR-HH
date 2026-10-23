package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.EmployeeShiftAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Detects recurring interval collisions without enumerating an open-ended date range. */
@Component
public class ShiftAssignmentConflictPolicy {
    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);

    public boolean conflicts(EmployeeShiftAssignment requested, WorkShift requestedShift,
            EmployeeShiftAssignment existing, WorkShift existingShift) {
        for (short requestedDay : requested.weekdays()) {
            LocalDate anchor = MONDAY.plusDays(requestedDay - 1);
            var requestedInterval = ShiftTimeRules.shiftInterval(requestedShift, anchor);
            for (int dayDifference = -1; dayDifference <= 1; dayDifference++) {
                short existingDay = (short) (Math.floorMod(requestedDay - 1 + dayDifference, 7) + 1);
                if (!existing.weekdays().contains(existingDay)
                        || !commonOccurrenceDate(requested, existing, requestedDay, dayDifference)) continue;
                var existingInterval = ShiftTimeRules.shiftInterval(existingShift,
                        anchor.plusDays(dayDifference));
                if (requestedInterval.overlaps(existingInterval)) return true;
            }
        }
        return false;
    }

    private boolean commonOccurrenceDate(EmployeeShiftAssignment requested,
            EmployeeShiftAssignment existing, short requestedDay, int existingDayDifference) {
        long earliest = Math.max(requested.startDate().toEpochDay(),
                existing.startDate().toEpochDay() - existingDayDifference);
        long latest = Math.min(requested.endDate() == null ? Long.MAX_VALUE : requested.endDate().toEpochDay(),
                existing.endDate() == null ? Long.MAX_VALUE
                        : existing.endDate().toEpochDay() - existingDayDifference);
        if (earliest > latest) return false;
        long weekday = Math.floorMod(earliest - MONDAY.toEpochDay(), 7) + 1;
        long nextOccurrence = earliest + Math.floorMod(requestedDay - weekday, 7);
        return nextOccurrence <= latest;
    }
}
