package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleDetermination;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleInterval;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.ShiftTimeRules;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftBreak;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftBreakRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class LeaveDayCalculator {
    private final EffectiveScheduleService schedules;
    private final WorkShiftRepository shifts;
    private final ShiftBreakRepository breaks;

    public LeaveDayCalculator(EffectiveScheduleService schedules, WorkShiftRepository shifts,
            ShiftBreakRepository breaks) {
        this.schedules = schedules; this.shifts = shifts; this.breaks = breaks;
    }

    public BigDecimal calculate(UUID employeeId, LocalDate from, LocalDate to,
            LocalTime start, LocalTime end) {
        if (from == null || to == null || to.isBefore(from)) throw new BusinessException("Invalid leave dates");
        boolean partial = start != null || end != null;
        if (partial && (start == null || end == null || !from.equals(to) || !end.isAfter(start)))
            throw new BusinessException("Partial leave requires one day and an increasing time interval");
        if (partial && (!wholeMinute(start) || !wholeMinute(end)))
            throw new BusinessException("Partial leave times must use whole minutes");
        Map<UUID, WorkShift> shiftCache = new HashMap<>();
        Map<UUID, List<ShiftBreak>> breakCache = new HashMap<>();
        BigDecimal days = BigDecimal.ZERO;
        for (LocalDate date = from; ; date = date.plusDays(1)) {
            var schedule = schedules.get(employeeId, date);
            if (schedule.determination() == ScheduleDetermination.INDETERMINATE)
                throw new BusinessException("Indeterminate work schedule on " + date + ": " + schedule.reason());
            List<ProgrammedInterval> intervals = new ArrayList<>();
            if (schedule.determination() == ScheduleDetermination.EXACT)
                for (ScheduleInterval interval : schedule.intervals())
                    intervals.add(new ProgrammedInterval(interval, date));
            if (partial && !date.equals(LocalDate.MIN)) {
                LocalDate previousDate = date.minusDays(1);
                var previous = schedules.get(employeeId, previousDate);
                if (previous.determination() == ScheduleDetermination.INDETERMINATE)
                    throw new BusinessException("Indeterminate previous work schedule on " + previousDate);
                if (previous.determination() == ScheduleDetermination.EXACT)
                    for (ScheduleInterval interval : previous.intervals())
                        if (interval.end().isAfter(date.atStartOfDay()))
                            intervals.add(new ProgrammedInterval(interval, previousDate));
            }
            if (intervals.isEmpty()) {
                if (partial) throw new BusinessException("No scheduled work on requested day");
                if (date.equals(to)) break;
                continue;
            }
            if (partial) {
                LocalDateTime partStart = date.atTime(start), partEnd = date.atTime(end);
                Set<LocalDate> startAnchors = new HashSet<>(), endAnchors = new HashSet<>();
                for (ProgrammedInterval programmed : intervals) {
                    ScheduleInterval i = programmed.interval();
                    if (!partStart.isBefore(i.start()) && partStart.isBefore(i.end()))
                        startAnchors.add(programmed.shiftDate());
                    if (partEnd.isAfter(i.start()) && !partEnd.isAfter(i.end()))
                        endAnchors.add(programmed.shiftDate());
                }
                startAnchors.retainAll(endAnchors);
                if (startAnchors.size() != 1)
                    throw new BusinessException("Partial leave must fit one scheduled workday");
                LocalDate anchor = startAnchors.iterator().next();
                intervals.removeIf(programmed -> !programmed.shiftDate().equals(anchor));
            }
            long total = 0;
            for (ProgrammedInterval programmed : intervals) {
                ScheduleInterval interval = programmed.interval();
                if (partial && (!wholeMinute(interval.start().toLocalTime())
                        || !wholeMinute(interval.end().toLocalTime())))
                    throw new BusinessException("Scheduled shift needs minute-precise times");
                WorkShift shift = shiftCache.computeIfAbsent(interval.shiftId(),
                        id -> shifts.findById(id).orElseThrow(() -> new BusinessException("Scheduled shift unavailable")));
                List<ShiftTimeRules.LocalInterval> unpaid = new ArrayList<>();
                for (ShiftBreak b : breakCache.computeIfAbsent(shift.id(), breaks::findAllByShiftId)) {
                    if (!b.paid()) {
                        if (partial && (!wholeMinute(b.startTime()) || !wholeMinute(b.endTime())))
                            throw new BusinessException("Scheduled break needs minute-precise times");
                        unpaid.add(ShiftTimeRules.breakInterval(shift, b, programmed.shiftDate()));
                    }
                }
                unpaid.sort(Comparator.comparing(ShiftTimeRules.LocalInterval::start));
                for (int i = 1; i < unpaid.size(); i++) {
                    if (unpaid.get(i - 1).overlaps(unpaid.get(i)))
                        throw new BusinessException("Overlapping unpaid breaks in schedule");
                }
                long scheduled = minutes(interval.start(), interval.end());
                for (var b : unpaid) scheduled -= minutes(b.start(), b.end());
                if (scheduled <= 0) throw new BusinessException("Scheduled work has no paid minutes");
                total += scheduled;
                if (partial) {
                    LocalDateTime partStart = date.atTime(start), partEnd = date.atTime(end);
                    long overlap = intersection(partStart, partEnd, interval.start(), interval.end());
                    for (var b : unpaid) overlap -= intersection(partStart, partEnd, b.start(), b.end());
                    if (overlap < 0) throw new BusinessException("Invalid scheduled breaks");
                }
            }
            if (partial) {
                LocalDateTime partStart = date.atTime(start), partEnd = date.atTime(end);
                boolean startsInShift = intervals.stream().map(ProgrammedInterval::interval)
                        .anyMatch(i -> !partStart.isBefore(i.start()) && partStart.isBefore(i.end()));
                boolean endsInShift = intervals.stream().map(ProgrammedInterval::interval)
                        .anyMatch(i -> partEnd.isAfter(i.start()) && !partEnd.isAfter(i.end()));
                if (!startsInShift || !endsInShift)
                    throw new BusinessException("Partial leave is outside scheduled work");
                long requested = 0;
                for (ProgrammedInterval programmed : intervals) {
                    ScheduleInterval i = programmed.interval();
                    WorkShift shift = shiftCache.get(i.shiftId());
                    long part = intersection(partStart, partEnd, i.start(), i.end());
                    for (ShiftBreak b : breakCache.get(shift.id())) {
                        if (!b.paid()) {
                            var bi = ShiftTimeRules.breakInterval(shift, b, programmed.shiftDate());
                            part -= intersection(partStart, partEnd, bi.start(), bi.end());
                        }
                    }
                    requested += Math.max(0, part);
                }
                if (requested <= 0 || total <= 0)
                    throw new BusinessException("Partial leave has no working minutes");
                days = BigDecimal.valueOf(requested).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            } else if (total > 0) days = days.add(BigDecimal.ONE);
            if (date.equals(to)) break;
        }
        if (days.signum() <= 0) throw new BusinessException("Leave has no chargeable working days");
        if (days.precision() > 8) throw new BusinessException("Leave duration exceeds database precision");
        return days.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean nightContinuationOverlaps(UUID employeeId, LocalDate shiftDate,
            LocalDate partialDate, LocalTime start, LocalTime end) {
        if (shiftDate.equals(LocalDate.MAX) || !shiftDate.plusDays(1).equals(partialDate)) return false;
        var schedule = schedules.get(employeeId, shiftDate);
        if (schedule.determination() == ScheduleDetermination.INDETERMINATE)
            throw new BusinessException("Cannot resolve overnight shift overlap");
        if (schedule.determination() != ScheduleDetermination.EXACT) return false;
        LocalDateTime from = partialDate.atTime(start), to = partialDate.atTime(end);
        for (ScheduleInterval interval : schedule.intervals()) {
            long productive = intersection(from, to, interval.start(), interval.end());
            if (productive <= 0) continue;
            WorkShift shift = shifts.findById(interval.shiftId())
                    .orElseThrow(() -> new BusinessException("Scheduled shift unavailable"));
            for (ShiftBreak shiftBreak : breaks.findAllByShiftId(shift.id()))
                if (!shiftBreak.paid()) {
                    var rest = ShiftTimeRules.breakInterval(shift, shiftBreak, shiftDate);
                    productive -= intersection(from, to, rest.start(), rest.end());
                }
            if (productive > 0) return true;
        }
        return false;
    }

    public boolean hasWorkingMinutes(UUID employeeId, LocalDate date, LocalTime start, LocalTime end) {
        if (start == null || end == null || !end.isAfter(start)) return false;
        List<ProgrammedInterval> intervals = new ArrayList<>();
        var current = schedules.get(employeeId, date);
        if (current.determination() == ScheduleDetermination.INDETERMINATE)
            throw new BusinessException("Cannot resolve partial leave overlap");
        if (current.determination() == ScheduleDetermination.EXACT)
            for (ScheduleInterval interval : current.intervals())
                intervals.add(new ProgrammedInterval(interval, date));
        if (!date.equals(LocalDate.MIN)) {
            LocalDate previousDate = date.minusDays(1);
            var previous = schedules.get(employeeId, previousDate);
            if (previous.determination() == ScheduleDetermination.INDETERMINATE)
                throw new BusinessException("Cannot resolve overnight leave overlap");
            if (previous.determination() == ScheduleDetermination.EXACT)
                for (ScheduleInterval interval : previous.intervals())
                    if (interval.end().isAfter(date.atStartOfDay()))
                        intervals.add(new ProgrammedInterval(interval, previousDate));
        }
        LocalDateTime from = date.atTime(start), to = date.atTime(end);
        for (ProgrammedInterval programmed : intervals) {
            ScheduleInterval interval = programmed.interval();
            long productive = intersection(from, to, interval.start(), interval.end());
            if (productive <= 0) continue;
            WorkShift shift = shifts.findById(interval.shiftId())
                    .orElseThrow(() -> new BusinessException("Scheduled shift unavailable"));
            for (ShiftBreak shiftBreak : breaks.findAllByShiftId(shift.id()))
                if (!shiftBreak.paid()) {
                    var rest = ShiftTimeRules.breakInterval(shift, shiftBreak, programmed.shiftDate());
                    productive -= intersection(from, to, rest.start(), rest.end());
                }
            if (productive > 0) return true;
        }
        return false;
    }

    private long minutes(LocalDateTime a, LocalDateTime b) { return Duration.between(a, b).toMinutes(); }
    private boolean wholeMinute(LocalTime value) {
        return value != null && value.getSecond() == 0 && value.getNano() == 0;
    }
    private long intersection(LocalDateTime a, LocalDateTime b, LocalDateTime c, LocalDateTime d) {
        LocalDateTime start = max(a, c), end = min(b, d);
        return end.isAfter(start) ? minutes(start, end) : 0;
    }
    private LocalDateTime max(LocalDateTime a, LocalDateTime b) { return a.isAfter(b) ? a : b; }
    private LocalDateTime min(LocalDateTime a, LocalDateTime b) { return a.isBefore(b) ? a : b; }
    private record ProgrammedInterval(ScheduleInterval interval, LocalDate shiftDate) { }
}
