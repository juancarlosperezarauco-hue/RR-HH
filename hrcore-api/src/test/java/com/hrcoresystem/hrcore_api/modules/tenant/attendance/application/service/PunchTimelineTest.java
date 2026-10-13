package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.PunchType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PunchTimelineTest {
    private PunchTimeline.Event event(PunchType type, String at) {
        return new PunchTimeline.Event(UUID.randomUUID(), type, Instant.parse(at));
    }

    @Test
    void actualUnpaidBreakAndScheduledBreakAreDeductedOnce() {
        var events = List.of(
                event(PunchType.IN, "2026-01-12T08:00:00Z"),
                event(PunchType.BREAK_START, "2026-01-12T12:05:00Z"),
                event(PunchType.BREAK_END, "2026-01-12T12:35:00Z"),
                event(PunchType.OUT, "2026-01-12T16:00:00Z"));
        var breaks = List.of(new PunchTimeline.Window(Instant.parse("2026-01-12T12:00:00Z"),
                Instant.parse("2026-01-12T12:30:00Z"), false));
        var result = PunchTimeline.evaluate(events, breaks);
        assertTrue(result.valid());
        assertTrue(result.complete());
        assertEquals(445, result.workedMinutes());
    }

    @Test
    void paidBreakDoesNotReduceWorkedMinutes() {
        var events = List.of(event(PunchType.IN, "2026-01-12T08:00:00Z"),
                event(PunchType.BREAK_START, "2026-01-12T10:00:00Z"),
                event(PunchType.BREAK_END, "2026-01-12T10:15:00Z"),
                event(PunchType.OUT, "2026-01-12T16:00:00Z"));
        var breaks = List.of(new PunchTimeline.Window(Instant.parse("2026-01-12T10:00:00Z"),
                Instant.parse("2026-01-12T10:15:00Z"), true));
        assertEquals(480, PunchTimeline.evaluate(events, breaks).workedMinutes());
    }

    @Test
    void multipleSegmentsAndOvernightPunchesAreSummed() {
        var events = List.of(event(PunchType.IN, "2026-01-12T22:00:00Z"),
                event(PunchType.OUT, "2026-01-13T02:00:00Z"),
                event(PunchType.IN, "2026-01-13T02:30:00Z"),
                event(PunchType.OUT, "2026-01-13T06:00:00Z"));
        assertEquals(450, PunchTimeline.evaluate(events, List.of()).workedMinutes());
    }

    @Test
    void invalidSequenceAndUnknownBreakPayDoNotInventDuration() {
        var duplicate = List.of(event(PunchType.IN, "2026-01-12T08:00:00Z"),
                event(PunchType.IN, "2026-01-12T08:01:00Z"));
        assertFalse(PunchTimeline.validSequencePrefix(duplicate));
        assertEquals("DUPLICATE_IN", PunchTimeline.evaluate(duplicate, List.of()).reason());

        var unknown = List.of(event(PunchType.IN, "2026-01-12T08:00:00Z"),
                event(PunchType.BREAK_START, "2026-01-12T10:00:00Z"),
                event(PunchType.BREAK_END, "2026-01-12T10:15:00Z"),
                event(PunchType.OUT, "2026-01-12T16:00:00Z"));
        var result = PunchTimeline.evaluate(unknown, List.of());
        assertFalse(result.valid());
        assertEquals(0, result.workedMinutes());
        assertEquals("BREAK_PAY_UNKNOWN", result.reason());
    }

    @Test
    void openPunchSequenceIsNotACompletedWorkDuration() {
        var result = PunchTimeline.evaluate(List.of(event(PunchType.IN, "2026-01-12T08:00:00Z")), List.of());
        assertTrue(result.valid());
        assertFalse(result.complete());
        assertEquals(0, result.workedMinutes());
    }
}
