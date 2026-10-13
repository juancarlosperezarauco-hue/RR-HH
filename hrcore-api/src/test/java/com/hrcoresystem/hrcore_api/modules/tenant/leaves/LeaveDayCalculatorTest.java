package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.LeaveDayCalculator;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LeaveDayCalculatorTest {
    private final UUID employee = UUID.randomUUID(), shiftId = UUID.randomUUID();
    private final EffectiveScheduleService schedules = mock(EffectiveScheduleService.class);
    private final WorkShiftRepository shifts = mock(WorkShiftRepository.class);
    private final ShiftBreakRepository breaks = mock(ShiftBreakRepository.class);
    private final LeaveDayCalculator calculator = new LeaveDayCalculator(schedules, shifts, breaks);

    @Test void usesProgrammedDaysIncludingWeekendsAndNightShift() {
        LocalDate friday = LocalDate.of(2026, 10, 16);
        WorkShift night = shift("22:00", "06:00", true);
        when(shifts.findById(shiftId)).thenReturn(Optional.of(night));
        when(breaks.findAllByShiftId(shiftId)).thenReturn(List.of());
        when(schedules.get(employee, friday)).thenReturn(exact(friday, night));
        when(schedules.get(employee, friday.plusDays(1))).thenReturn(noWork(friday.plusDays(1)));
        when(schedules.get(employee, friday.plusDays(2))).thenReturn(exact(friday.plusDays(2), night));
        assertEquals(new BigDecimal("2.00"), calculator.calculate(employee, friday,
                friday.plusDays(2), null, null));
    }

    @Test void partialDaysSubtractUnpaidBreaksAndRoundHalfUp() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        WorkShift day = shift("09:00", "18:00", false);
        when(shifts.findById(shiftId)).thenReturn(Optional.of(day));
        when(breaks.findAllByShiftId(shiftId)).thenReturn(List.of(new ShiftBreak(UUID.randomUUID(),
                shiftId, "Lunch", LocalTime.NOON, LocalTime.of(13, 0), 60, false, null)));
        when(schedules.get(employee, date)).thenReturn(exact(date, day));
        when(schedules.get(employee, date.minusDays(1))).thenReturn(noWork(date.minusDays(1)));
        assertEquals(new BigDecimal("0.25"), calculator.calculate(employee, date, date,
                LocalTime.of(9, 0), LocalTime.of(11, 0)));
        assertEquals(new BigDecimal("0.13"), calculator.calculate(employee, date, date,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertFalse(calculator.hasWorkingMinutes(employee, date, LocalTime.NOON, LocalTime.of(13, 0)));
        assertTrue(calculator.hasWorkingMinutes(employee, date, LocalTime.of(11, 30), LocalTime.of(12, 30)));
        assertThrows(BusinessException.class, () -> calculator.calculate(employee, date, date,
                LocalTime.of(9, 0), LocalTime.of(9, 1)));
        assertThrows(BusinessException.class, () -> calculator.calculate(employee, date, date,
                LocalTime.of(9, 0, 30), LocalTime.of(10, 0)));
    }

    @Test void rejectsIndeterminateAndUnrepresentablePartialSchedules() {
        LocalDate date = LocalDate.of(2026, 10, 13);
        when(schedules.get(employee, date)).thenReturn(new ScheduleResponse(employee, date, null,
                null, "ORGANIZATION_EXCEPTION", ScheduleDetermination.INDETERMINATE,
                "SCHEDULE_CHANGE_WITHOUT_HOURS", List.of()));
        assertThrows(BusinessException.class, () -> calculator.calculate(employee, date, date, null, null));
        assertThrows(BusinessException.class, () -> calculator.calculate(employee, date, date,
                LocalTime.of(22, 0), LocalTime.of(6, 0)));
    }

    @Test void representsHoursAfterMidnightFromPreviousNightShift() {
        LocalDate date = LocalDate.of(2026, 10, 13), previous = date.minusDays(1);
        WorkShift night = shift("22:00", "06:00", true);
        when(shifts.findById(shiftId)).thenReturn(Optional.of(night));
        when(breaks.findAllByShiftId(shiftId)).thenReturn(List.of());
        when(schedules.get(employee, previous)).thenReturn(exact(previous, night));
        when(schedules.get(employee, date)).thenReturn(noWork(date));
        assertEquals(new BigDecimal("0.25"), calculator.calculate(employee, date, date,
                LocalTime.of(2, 0), LocalTime.of(4, 0)));
        when(schedules.get(employee, date)).thenReturn(exact(date, night));
        assertEquals(new BigDecimal("0.25"), calculator.calculate(employee, date, date,
                LocalTime.of(2, 0), LocalTime.of(4, 0)));
        assertTrue(calculator.nightContinuationOverlaps(employee, previous, date,
                LocalTime.of(2, 0), LocalTime.of(4, 0)));
        assertFalse(calculator.nightContinuationOverlaps(employee, previous, date,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    private WorkShift shift(String start, String end, boolean night) {
        return new WorkShift(shiftId, "S", "Shift", null, LocalTime.parse(start), LocalTime.parse(end),
                night, 0, 0, ShiftStatus.ACTIVE, null, null, null);
    }
    private ScheduleResponse exact(LocalDate date, WorkShift shift) {
        LocalDateTime start = date.atTime(shift.startTime());
        LocalDateTime end = date.plusDays(shift.crossesMidnight() ? 1 : 0).atTime(shift.endTime());
        return new ScheduleResponse(employee, date, null, null, "USUAL_ASSIGNMENT",
                ScheduleDetermination.EXACT, null, List.of(new ScheduleInterval(shiftId, start, end)));
    }
    private ScheduleResponse noWork(LocalDate date) {
        return new ScheduleResponse(employee, date, null, null, "USUAL_ASSIGNMENT",
                ScheduleDetermination.NO_WORK, null, List.of());
    }
}
