package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftBreak;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShiftTimeRulesTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);

    @Test
    void normalNightAndFullDayIntervals() {
        assertEquals(new ShiftTimeRules.LocalInterval(
                        LocalDateTime.of(2026, 10, 13, 9, 0),
                        LocalDateTime.of(2026, 10, 13, 17, 0)),
                ShiftTimeRules.shiftInterval(shift("09:00", "17:00", false), DAY));
        assertEquals(new ShiftTimeRules.LocalInterval(
                        LocalDateTime.of(2026, 10, 13, 22, 0),
                        LocalDateTime.of(2026, 10, 14, 6, 0)),
                ShiftTimeRules.shiftInterval(shift("22:00", "06:00", true), DAY));
        assertEquals(new ShiftTimeRules.LocalInterval(
                        LocalDateTime.of(2026, 10, 13, 9, 0),
                        LocalDateTime.of(2026, 10, 14, 9, 0)),
                ShiftTimeRules.shiftInterval(shift("09:00", "09:00", true), DAY));
    }

    @Test
    void rejectsTimesThatConflictWithMidnightFlag() {
        assertThrows(BusinessException.class,
                () -> ShiftTimeRules.validateShift(LocalTime.NOON, LocalTime.NOON, false));
        assertThrows(BusinessException.class,
                () -> ShiftTimeRules.validateShift(LocalTime.NOON, LocalTime.MIDNIGHT, false));
        assertThrows(BusinessException.class,
                () -> ShiftTimeRules.validateShift(LocalTime.MIDNIGHT, LocalTime.NOON, true));
    }

    @Test
    void breakAcrossMidnightMustHaveExactDurationAndRemainInsideShift() {
        WorkShift shift = shift("22:00", "06:00", true);
        ShiftBreak valid = shiftBreak("23:45", "00:15", 30);
        assertEquals(LocalDateTime.of(2026, 10, 14, 0, 15),
                ShiftTimeRules.breakInterval(shift, valid, DAY).end());
        assertThrows(BusinessException.class,
                () -> ShiftTimeRules.breakInterval(shift, shiftBreak("23:45", "00:15", 31), DAY));
        assertThrows(BusinessException.class,
                () -> ShiftTimeRules.breakInterval(shift, shiftBreak("05:45", "06:15", 30), DAY));
    }

    @Test
    void neighboringHalfOpenBreaksDoNotOverlap() {
        WorkShift shift = shift("22:00", "06:00", true);
        var first = ShiftTimeRules.breakInterval(shift, shiftBreak("23:30", "00:00", 30), DAY);
        var second = ShiftTimeRules.breakInterval(shift, shiftBreak("00:00", "00:30", 30), DAY);
        assertFalse(first.overlaps(second));
    }

    private WorkShift shift(String start, String end, boolean crossesMidnight) {
        return new WorkShift(UUID.randomUUID(), "N1", "Night", null,
                LocalTime.parse(start), LocalTime.parse(end), crossesMidnight,
                0, 0, ShiftStatus.ACTIVE, null, null, null);
    }

    private ShiftBreak shiftBreak(String start, String end, int minutes) {
        return new ShiftBreak(UUID.randomUUID(), UUID.randomUUID(), "Rest",
                LocalTime.parse(start), LocalTime.parse(end), minutes, false, null);
    }
}
