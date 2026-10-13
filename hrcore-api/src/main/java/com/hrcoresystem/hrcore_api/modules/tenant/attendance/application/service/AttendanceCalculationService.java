package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleDetermination;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.ShiftTimeRules;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftBreakRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceCalculationService {
    private static final Set<String> SORT = Set.of("workDate", "status", "createdAt", "updatedAt", "employeeId");
    private final AttendanceStore store;
    private final EmployeeRepository employees;
    private final EffectiveScheduleService schedules;
    private final ShiftAssignmentRepository assignments;
    private final WorkShiftRepository shifts;
    private final ShiftBreakRepository breaks;
    private final AttendanceZone zones;
    private final TenantPlanEnforcementService plans;
    private final AuditTrailService audit;
    private final AttendanceMapper mapper;
    private final ObjectMapper json;

    public AttendanceCalculationService(AttendanceStore store, EmployeeRepository employees,
            EffectiveScheduleService schedules, ShiftAssignmentRepository assignments,
            WorkShiftRepository shifts, ShiftBreakRepository breaks, AttendanceZone zones,
            TenantPlanEnforcementService plans, AuditTrailService audit, AttendanceMapper mapper,
            ObjectMapper json) {
        this.store = store; this.employees = employees; this.schedules = schedules;
        this.assignments = assignments; this.shifts = shifts; this.breaks = breaks;
        this.zones = zones; this.plans = plans; this.audit = audit; this.mapper = mapper; this.json = json;
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponses.Record> list(UUID employeeId, LocalDate from, LocalDate to,
            AttendanceStatus status, int page, int size, String sort, String direction) {
        plans.assertCanOperate();
        if (from != null && to != null && from.isAfter(to)) throw new BusinessException("from must not exceed to");
        return store.records(employeeId, from, to, status,
                AttendancePage.of(page, size, sort, direction, "workDate", SORT)).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceResponses.Record detail(UUID id) {
        plans.assertCanOperate();
        return mapper.toResponse(store.record(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id)));
    }

    @Transactional
    public AttendanceResponses.Record calculate(UUID employeeId, LocalDate workDate) {
        plans.assertCanOperate();
        if (employeeId == null || workDate == null) throw new BusinessException("employeeId and workDate are required");
        zones.required();
        employees.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        AttendanceRecord record = store.lockOrCreate(employeeId, workDate);
        return mapper.toResponse(recalculateLocked(record));
    }

    /** Caller holds the employee row lock and an open transaction. */
    public AttendanceRecord recalculateLocked(AttendanceRecord before) {
        ZoneId zone = zones.required();
        ScheduleResponse schedule = schedules.get(before.employeeId(), before.workDate());
        List<AttendancePunch> punches = store.punches(before.id());
        List<AttendanceAdjustment> approved = store.adjustments(before.id()).stream()
                .filter(a -> a.status() == ReviewStatus.APPROVED)
                .sorted(Comparator.comparing(AttendanceAdjustment::appliedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(AttendanceAdjustment::id))
                .toList();
        List<PunchTimeline.Event> events = new ArrayList<>();
        for (AttendancePunch punch : punches) events.add(new PunchTimeline.Event(punch.id(), punch.punchType(), punch.punchedAt()));
        AttendanceStatus override = null;
        String reason = null;
        try {
            for (AttendanceAdjustment adjustment : approved) {
                JsonNode v = adjustment.requestedValues();
                switch (adjustment.adjustmentType()) {
                    case MISSING_PUNCH -> events.add(new PunchTimeline.Event(adjustment.id(),
                            PunchType.valueOf(v.path("punchType").asText()), Instant.parse(v.path("punchedAt").asText())));
                    case TIME_CORRECTION -> {
                        UUID punchId = UUID.fromString(v.path("punchId").asText());
                        Instant corrected = Instant.parse(v.path("punchedAt").asText());
                        if (events.stream().noneMatch(e -> punchId.equals(e.id()))) throw new IllegalArgumentException();
                        events.replaceAll(e -> punchId.equals(e.id()) ? new PunchTimeline.Event(e.id(), e.type(), corrected) : e);
                    }
                    case STATUS_CORRECTION -> override = AttendanceStatus.valueOf(v.path("status").asText());
                    case OTHER -> override = AttendanceStatus.JUSTIFIED;
                }
            }
        } catch (RuntimeException invalidAdjustment) {
            reason = "INVALID_APPROVED_ADJUSTMENT";
        }

        UUID assignmentId = null;
        Instant expectedStart = null, expectedEnd = null, actualFirst = null, actualLast = null;
        int worked = 0, late = 0, early = 0;
        AttendanceStatus status;
        Optional<WorkShift> shift = Optional.empty();
        List<PunchTimeline.Window> scheduledBreaks = new ArrayList<>();
        if (reason != null) status = AttendanceStatus.INCOMPLETE;
        else if (schedule.determination() == ScheduleDetermination.INDETERMINATE) {
            status = AttendanceStatus.INCOMPLETE; reason = schedule.reason();
        } else if (schedule.determination() == ScheduleDetermination.NO_WORK) {
            if (schedule.calendarExceptionId() != null) {
                status = events.isEmpty() ? AttendanceStatus.DAY_OFF : AttendanceStatus.INCOMPLETE;
                if (!events.isEmpty()) reason = "PUNCH_ON_DAY_OFF";
            } else {
                status = AttendanceStatus.PENDING; reason = "NO_CONFIRMED_SCHEDULE";
            }
        } else {
            var effective = assignments.findEffectiveOn(before.employeeId(), before.workDate());
            if (effective.size() != 1 || schedule.intervals().size() != 1) {
                status = AttendanceStatus.INCOMPLETE; reason = "MULTIPLE_OR_MISSING_ASSIGNMENTS";
            } else {
                var assignment = effective.getFirst();
                assignmentId = assignment.id();
                shift = shifts.findById(assignment.shiftId());
                if (shift.isEmpty() || !schedule.intervals().getFirst().shiftId().equals(assignment.shiftId())) {
                    status = AttendanceStatus.INCOMPLETE; reason = "SHIFT_NOT_AVAILABLE";
                } else {
                    try {
                        expectedStart = zones.uniqueInstant(schedule.intervals().getFirst().start(), zone);
                        expectedEnd = zones.uniqueInstant(schedule.intervals().getFirst().end(), zone);
                        for (var pause : breaks.findAllByShiftId(shift.get().id())) {
                            var local = ShiftTimeRules.breakInterval(shift.get(), pause, before.workDate());
                            scheduledBreaks.add(new PunchTimeline.Window(zones.uniqueInstant(local.start(), zone),
                                    zones.uniqueInstant(local.end(), zone), pause.paid()));
                        }
                    } catch (AttendanceZone.IndeterminateScheduleTime | BusinessException ex) {
                        reason = "AMBIGUOUS_OR_INVALID_LOCAL_SCHEDULE";
                    }
                    if (reason != null || !expectedEnd.isAfter(expectedStart)) {
                        status = AttendanceStatus.INCOMPLETE;
                        if (reason == null) reason = "INVALID_SCHEDULE_DURATION";
                    } else if (events.isEmpty()) {
                        status = Instant.now().isAfter(expectedEnd) ? AttendanceStatus.ABSENT : AttendanceStatus.PENDING;
                    } else {
                        PunchTimeline.Result result = PunchTimeline.evaluate(events, scheduledBreaks);
                        actualFirst = result.firstIn(); actualLast = result.lastOut();
                        late = positiveMinutes(expectedStart, actualFirst);
                        early = positiveMinutes(actualLast, expectedEnd);
                        if (!result.valid()) {
                            status = AttendanceStatus.INCOMPLETE; reason = result.reason();
                        } else if (!result.complete()) {
                            status = AttendanceStatus.INCOMPLETE;
                            reason = result.reason();
                        } else if (!Instant.now().isAfter(expectedEnd)) {
                            status = AttendanceStatus.PENDING;
                            worked = result.workedMinutes();
                        } else {
                            worked = result.workedMinutes();
                            status = late > shift.get().lateToleranceMinutes() ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
                        }
                    }
                }
            }
        }
        if (override != null && (status != AttendanceStatus.INCOMPLETE || override == AttendanceStatus.JUSTIFIED)
                && !"INVALID_APPROVED_ADJUSTMENT".equals(reason)) status = override;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("version", 1); details.put("timezone", zone.getId());
        details.put("scheduleSource", schedule.source()); details.put("scheduleDetermination", schedule.determination());
        details.put("calendarExceptionId", schedule.calendarExceptionId()); details.put("assignmentId", assignmentId);
        details.put("shiftId", shift.map(v -> v.id()).orElse(null)); details.put("reason", reason);
        details.put("lateToleranceMinutes", shift.map(v -> v.lateToleranceMinutes()).orElse(null));
        details.put("earlyDepartureToleranceMinutes", shift.map(v -> v.earlyDepartureToleranceMinutes()).orElse(null));
        details.put("scheduledBreaks", scheduledBreaks); details.put("appliedAdjustmentIds", approved.stream().map(AttendanceAdjustment::id).toList());
        JsonNode calculationDetails = json.valueToTree(details);
        AttendanceRecord updated = new AttendanceRecord(before.id(), before.employeeId(), assignmentId, before.workDate(),
                expectedStart, expectedEnd, actualFirst, actualLast, worked, late, early, status,
                calculationDetails, Instant.now(), before.createdAt(), before.updatedAt());
        if (same(before, updated)) return before;
        AttendanceRecord saved = store.save(updated);
        audit.recordTenantEvent(before.calculatedAt() == null ? AuditEventTypes.ATTENDANCE_RECORD_CALCULATED
                : AuditEventTypes.ATTENDANCE_RECORD_RECALCULATED, "ATTENDANCE_RECORD", saved.id().toString(),
                Map.of("employeeId", saved.employeeId().toString(), "workDate", saved.workDate().toString()),
                mapper.toResponse(before), mapper.toResponse(saved));
        return saved;
    }

    private int positiveMinutes(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) return 0;
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes > Integer.MAX_VALUE) throw new BusinessException("Attendance duration exceeds integer range");
        return (int) minutes;
    }

    private boolean same(AttendanceRecord a, AttendanceRecord b) {
        return Objects.equals(a.shiftAssignmentId(), b.shiftAssignmentId())
                && Objects.equals(a.expectedStartAt(), b.expectedStartAt())
                && Objects.equals(a.expectedEndAt(), b.expectedEndAt())
                && Objects.equals(a.actualFirstInAt(), b.actualFirstInAt())
                && Objects.equals(a.actualLastOutAt(), b.actualLastOutAt())
                && a.workedMinutes() == b.workedMinutes() && a.lateMinutes() == b.lateMinutes()
                && a.earlyDepartureMinutes() == b.earlyDepartureMinutes() && a.status() == b.status()
                && Objects.equals(a.calculationDetails(), b.calculationDetails());
    }
}
