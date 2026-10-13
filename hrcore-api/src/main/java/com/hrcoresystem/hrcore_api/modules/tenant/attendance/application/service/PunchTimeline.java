package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.PunchType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Evaluates immutable punch evidence and paid/unpaid scheduled breaks. */
final class PunchTimeline {
    private PunchTimeline() { }

    record Event(UUID id, PunchType type, Instant at) { }
    record Window(Instant start, Instant end, boolean paid) { }
    record Interval(Instant start, Instant end) {
        boolean overlaps(Interval other) { return start.isBefore(other.end) && other.start.isBefore(end); }
    }
    record Result(boolean valid, boolean complete, String reason, Instant firstIn,
                  Instant lastOut, int workedMinutes) { }

    static boolean validSequencePrefix(List<Event> raw) {
        List<Event> events = raw.stream().sorted(Comparator.comparing(Event::at).thenComparing(Event::id)).toList();
        boolean inside = false, onBreak = false;
        Instant inAt = null, breakAt = null;
        for (Event e : events) {
            if (e.at() == null || e.type() == null) return false;
            switch (e.type()) {
                case IN -> { if (inside) return false; inside = true; inAt = e.at(); }
                case OUT -> {
                    if (!inside || onBreak || !e.at().isAfter(inAt)) return false;
                    inside = false; inAt = null;
                }
                case BREAK_START -> {
                    if (!inside || onBreak || !e.at().isAfter(inAt)) return false;
                    onBreak = true; breakAt = e.at();
                }
                case BREAK_END -> {
                    if (!onBreak || !e.at().isAfter(breakAt)) return false;
                    onBreak = false; breakAt = null;
                }
            }
        }
        return true;
    }

    static Result evaluate(List<Event> raw, List<Window> scheduled) {
        List<Event> events = raw.stream().sorted(Comparator.comparing(Event::at).thenComparing(Event::id)).toList();
        List<Interval> work = new ArrayList<>();
        List<Interval> actualBreaks = new ArrayList<>();
        Instant firstIn = null, lastOut = null, inAt = null, breakAt = null;
        boolean inside = false, onBreak = false;
        for (Event event : events) {
            if (event.at() == null || event.type() == null) return invalid("INVALID_PUNCH", firstIn, lastOut);
            switch (event.type()) {
                case IN -> {
                    if (inside) return invalid("DUPLICATE_IN", firstIn, lastOut);
                    inside = true; inAt = event.at();
                    if (firstIn == null) firstIn = inAt;
                }
                case OUT -> {
                    if (!inside || onBreak || !event.at().isAfter(inAt))
                        return invalid("UNMATCHED_OUT", firstIn, lastOut);
                    work.add(new Interval(inAt, event.at()));
                    lastOut = event.at(); inside = false; inAt = null;
                }
                case BREAK_START -> {
                    if (!inside || onBreak || !event.at().isAfter(inAt))
                        return invalid("INVALID_BREAK_START", firstIn, lastOut);
                    onBreak = true; breakAt = event.at();
                }
                case BREAK_END -> {
                    if (!onBreak || !event.at().isAfter(breakAt))
                        return invalid("UNMATCHED_BREAK_END", firstIn, lastOut);
                    actualBreaks.add(new Interval(breakAt, event.at()));
                    onBreak = false; breakAt = null;
                }
            }
        }
        if (onBreak) return new Result(true, false, "OPEN_BREAK", firstIn, lastOut, 0);
        if (inside) return new Result(true, false, "OPEN_IN", firstIn, lastOut, 0);
        if (work.isEmpty()) return new Result(true, true, null, firstIn, lastOut, 0);

        List<Interval> deduct = new ArrayList<>();
        for (Window planned : scheduled) {
            if (planned.start() == null || planned.end() == null || !planned.end().isAfter(planned.start()))
                return invalid("INVALID_SCHEDULED_BREAK", firstIn, lastOut);
            if (!planned.paid()) {
                List<Interval> uncovered = List.of(new Interval(planned.start(), planned.end()));
                for (Interval actual : actualBreaks) {
                    List<Interval> remaining = new ArrayList<>();
                    for (Interval part : uncovered) remaining.addAll(without(part, actual));
                    uncovered = remaining;
                }
                deduct.addAll(uncovered);
            }
        }
        for (Interval actual : actualBreaks) {
            List<Window> matched = scheduled.stream()
                    .filter(planned -> actual.overlaps(new Interval(planned.start(), planned.end()))).toList();
            if (matched.size() != 1) return invalid("BREAK_PAY_UNKNOWN", firstIn, lastOut);
            if (!matched.getFirst().paid()) deduct.add(actual);
        }

        long workedSeconds = 0;
        for (Interval segment : work) {
            long seconds = Duration.between(segment.start(), segment.end()).getSeconds();
            List<Interval> clipped = new ArrayList<>();
            for (Interval pause : deduct) {
                Instant start = pause.start().isAfter(segment.start()) ? pause.start() : segment.start();
                Instant end = pause.end().isBefore(segment.end()) ? pause.end() : segment.end();
                if (end.isAfter(start)) clipped.add(new Interval(start, end));
            }
            clipped.sort(Comparator.comparing(Interval::start));
            Instant currentStart = null, currentEnd = null;
            for (Interval pause : clipped) {
                if (currentStart == null) { currentStart = pause.start(); currentEnd = pause.end(); }
                else if (!pause.start().isAfter(currentEnd)) {
                    if (pause.end().isAfter(currentEnd)) currentEnd = pause.end();
                } else {
                    seconds -= Duration.between(currentStart, currentEnd).getSeconds();
                    currentStart = pause.start(); currentEnd = pause.end();
                }
            }
            if (currentStart != null) seconds -= Duration.between(currentStart, currentEnd).getSeconds();
            workedSeconds = Math.addExact(workedSeconds, seconds);
        }
        if (workedSeconds < 0 || workedSeconds / 60 > Integer.MAX_VALUE)
            return invalid("INVALID_WORK_DURATION", firstIn, lastOut);
        return new Result(true, true, null, firstIn, lastOut, (int) (workedSeconds / 60));
    }

    private static Result invalid(String reason, Instant firstIn, Instant lastOut) {
        return new Result(false, false, reason, firstIn, lastOut, 0);
    }

    private static List<Interval> without(Interval planned, Interval actual) {
        if (!planned.overlaps(actual)) return List.of(planned);
        List<Interval> remaining = new ArrayList<>(2);
        if (planned.start().isBefore(actual.start()))
            remaining.add(new Interval(planned.start(), actual.start()));
        if (actual.end().isBefore(planned.end()))
            remaining.add(new Interval(actual.end(), planned.end()));
        return remaining;
    }
}
