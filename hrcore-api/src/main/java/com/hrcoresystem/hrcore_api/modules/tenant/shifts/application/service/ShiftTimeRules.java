package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftBreak;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Local, half-open intervals. Conversion to an instant belongs to a tenant-specific timezone rule. */
public final class ShiftTimeRules {
    private ShiftTimeRules() { }

    public record LocalInterval(LocalDateTime start, LocalDateTime end) {
        public boolean overlaps(LocalInterval other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }

    public static void validateShift(LocalTime start, LocalTime end, boolean crossesMidnight) {
        if (start == null || end == null) {
            throw new BusinessException("Shift startTime and endTime are required");
        }
        if ((!crossesMidnight && !end.isAfter(start))
                || (crossesMidnight && end.isAfter(start))) {
            throw new BusinessException("Shift times are inconsistent with crossesMidnight");
        }
    }

    public static LocalInterval shiftInterval(WorkShift shift, LocalDate startDate) {
        validateShift(shift.startTime(), shift.endTime(), shift.crossesMidnight());
        LocalDateTime start = startDate.atTime(shift.startTime());
        LocalDateTime end = startDate.plusDays(shift.crossesMidnight() ? 1 : 0)
                .atTime(shift.endTime());
        return new LocalInterval(start, end);
    }

    public static LocalInterval breakInterval(WorkShift shift, ShiftBreak shiftBreak, LocalDate shiftDate) {
        if (shiftBreak.durationMinutes() < 1 || shiftBreak.durationMinutes() > 1440) {
            throw new BusinessException("Break durationMinutes must be between 1 and 1440");
        }
        if (shiftBreak.startTime() == null || shiftBreak.endTime() == null) {
            throw new BusinessException("Break startTime and endTime are required");
        }
        LocalInterval shiftHours = shiftInterval(shift, shiftDate);
        for (int offset = 0; offset <= 1; offset++) {
            LocalDate startDay = shiftDate.plusDays(offset);
            LocalDateTime start = startDay.atTime(shiftBreak.startTime());
            LocalDateTime end = startDay.plusDays(
                    shiftBreak.endTime().isAfter(shiftBreak.startTime()) ? 0 : 1)
                    .atTime(shiftBreak.endTime());
            if (Duration.between(start, end).equals(Duration.ofMinutes(shiftBreak.durationMinutes()))
                    && !start.isBefore(shiftHours.start())
                    && !end.isAfter(shiftHours.end())) {
                return new LocalInterval(start, end);
            }
        }
        throw new BusinessException("Break must fit within the shift and match durationMinutes exactly");
    }
}
