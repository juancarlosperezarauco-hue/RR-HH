package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmploymentPeriodService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.OrganizationalReportingService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleDetermination;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EffectiveScheduleService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.ShiftTimeRules;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftBreakRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AttendancePunchService {
    private static final Set<String> SORT = Set.of("punchedAt", "createdAt", "punchType", "source", "employeeId");
    private final AttendanceStore store;
    private final EmployeeRepository employees;
    private final AttendanceCalculationService calculations;
    private final EffectiveScheduleService schedules;
    private final ShiftAssignmentRepository assignments;
    private final WorkShiftRepository shifts;
    private final ShiftBreakRepository breaks;
    private final AttendanceZone zones;
    private final AttendanceActor actor;
    private final AttendanceMapper mapper;
    private final TenantPlanEnforcementService plans;
    private final AuditTrailService audit;
    private final EmploymentPeriodService periods;
    private final AttendancePolicyService policies;
    private final NotificationPublisherPort notifications;
    private final OrganizationalReportingService reporting;

    @Autowired
    public AttendancePunchService(AttendanceStore store, EmployeeRepository employees,
            AttendanceCalculationService calculations, EffectiveScheduleService schedules,
            ShiftAssignmentRepository assignments, WorkShiftRepository shifts, ShiftBreakRepository breaks,
            AttendanceZone zones, AttendanceActor actor, AttendanceMapper mapper,
            TenantPlanEnforcementService plans, AuditTrailService audit, EmploymentPeriodService periods,
            AttendancePolicyService policies, NotificationPublisherPort notifications, OrganizationalReportingService reporting) {
        this.store = store; this.employees = employees; this.calculations = calculations;
        this.schedules = schedules; this.assignments = assignments; this.shifts = shifts;
        this.breaks = breaks; this.zones = zones; this.actor = actor;
        this.mapper = mapper; this.plans = plans; this.audit = audit;
        this.periods = periods; this.policies = policies; this.notifications = notifications; this.reporting = reporting;
    }

    /** Constructor de compatibilidad para pruebas aisladas del módulo. */
    public AttendancePunchService(AttendanceStore store, EmployeeRepository employees,
            AttendanceCalculationService calculations, EffectiveScheduleService schedules,
            ShiftAssignmentRepository assignments, WorkShiftRepository shifts, ShiftBreakRepository breaks,
            AttendanceZone zones, AttendanceActor actor, AttendanceMapper mapper,
            TenantPlanEnforcementService plans, AuditTrailService audit) {
        this(store, employees, calculations, schedules, assignments, shifts, breaks, zones, actor, mapper,
                plans, audit, null, null, null, null);
    }

    @Transactional
    public AttendanceResponses.Punch create(AttendanceRequests.Punch request) {
        plans.assertCanOperate();
        UUID userId = actor.id();
        Optional<UUID> linkedEmployee = store.employeeForUser(userId);
        UUID employeeId = request.employeeId() == null ? linkedEmployee.orElseThrow(() ->
                new BusinessException("employeeId is required when the tenant user is not linked to an employee"))
                : request.employeeId();
        boolean own = linkedEmployee.filter(employeeId::equals).isPresent();
        if (own && request.punchedAt() == null && policies != null)
            policies.validateWebPunch(request.latitude(), request.longitude());
        boolean administrative = request.punchedAt() != null || !own;
        if (administrative && !actor.has("attendance.punches.manage"))
            throw new AccessDeniedException("Administrative punch permission is required");
        if (!administrative && !actor.has("attendance.punches.create")
                && !actor.has("attendance.punches.manage"))
            throw new AccessDeniedException("Punch permission is required");
        if (administrative && request.punchedAt() != null
                && (request.notes() == null || request.notes().isBlank()))
            throw new BusinessException("Historical punches require notes explaining the entry");
        Instant at = request.punchedAt() == null ? Instant.now() : request.punchedAt();
        if (at.isAfter(Instant.now().plusSeconds(60))) throw new BusinessException("Future punches are not allowed");
        ZoneId zone = zones.required();
        employees.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        Instant from = at.minus(Duration.ofHours(36));
        Instant to = at.plus(Duration.ofHours(36));
        List<AttendancePunch> nearby = store.punches(employeeId, from, to);
        for (AttendancePunch previous : nearby) {
            if (previous.punchType() == request.punchType()
                    && Math.abs(Duration.between(previous.punchedAt(), at).toSeconds()) < 10)
                throw new BusinessException("Duplicate punch within 10 seconds");
        }
        List<PunchTimeline.Event> sequence = new ArrayList<>(nearby.stream()
                .map(p -> new PunchTimeline.Event(p.id(), p.punchType(), p.punchedAt())).toList());
        sequence.add(new PunchTimeline.Event(UUID.randomUUID(), request.punchType(), at));
        if (!PunchTimeline.validSequencePrefix(sequence)) throw new BusinessException("Invalid punch sequence");

        Optional<LocalDate> workDate = workDateFor(employeeId, request.punchType(), at, nearby, zone);
        AttendanceRecord record = null;
        if (workDate.isPresent()) {
            LocalDate resolvedDate = workDate.get();
            record = periods == null
                    ? store.lockOrCreate(employeeId, resolvedDate)
                    : store.lockOrCreate(employeeId, resolvedDate, periods.requireForDate(employeeId, resolvedDate).id());
        }
        AttendancePunch saved = store.save(new AttendancePunch(null, employeeId,
                record == null ? null : record.id(), request.punchType(), at,
                administrative ? PunchSource.MANUAL : PunchSource.WEB, null,
                request.latitude(), request.longitude(), userId, request.notes(), null));
        audit.recordTenantEvent(AuditEventTypes.ATTENDANCE_PUNCH_RECORDED, "ATTENDANCE_PUNCH",
                saved.id().toString(), Map.of("employeeId", employeeId.toString(), "type", saved.punchType().name(),
                        "source", saved.source().name(), "recordId", record == null ? "" : record.id().toString()));
        if (record != null) {
            AttendanceRecord calculated = calculations.recalculateLocked(record);
            if (notifications != null && (calculated.status() == AttendanceStatus.LATE || calculated.status() == AttendanceStatus.INCOMPLETE)) {
                try {
                    employees.findById(employeeId).map(e -> e.tenantUserId()).ifPresent(target ->
                        notifyAfterCommit(new NotificationPublishRequest(target, NotificationType.SYSTEM_NOTICE,
                            NotificationCategory.SYSTEM, NotificationPriority.NORMAL, "Novedad en tu asistencia",
                            "Tu registro del " + calculated.workDate() + " presenta una novedad: " + attendanceStatusLabel(calculated.status()) + ".",
                            null, null, "/dashboard/attendance?recordId=" + calculated.id(), null)));
                    if (reporting != null) {
                        UUID supervisorId = reporting.resolveEffectiveSupervisor(employeeId, calculated.workDate());
                        if (supervisorId != null) employees.findById(supervisorId).map(e -> e.tenantUserId()).ifPresent(target ->
                            notifyAfterCommit(new NotificationPublishRequest(target, NotificationType.SYSTEM_NOTICE,
                                NotificationCategory.SYSTEM, NotificationPriority.NORMAL, "Novedad de asistencia",
                                "El registro del colaborador presenta una novedad: " + attendanceStatusLabel(calculated.status()) + ".",
                                null, null, "/dashboard/attendance?recordId=" + calculated.id(), null)));
                    }
                } catch (Exception ignored) { }
            }
        }
        return mapper.toResponse(saved);
    }

    private void notifyAfterCommit(NotificationPublishRequest payload) {
        if (notifications == null) return;
        Runnable publish = () -> {
            try { notifications.publish(payload); } catch (Exception ignored) { }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { publish.run(); }
        });
    }

    private String attendanceStatusLabel(AttendanceStatus status) {
        return switch (status) {
            case PENDING -> "pendiente";
            case PRESENT -> "presente";
            case LATE -> "atraso";
            case ABSENT -> "ausente";
            case JUSTIFIED -> "justificada";
            case INCOMPLETE -> "incompleta";
            case DAY_OFF -> "día libre";
        };
    }

    private Optional<LocalDate> workDateFor(UUID employeeId, PunchType type, Instant at,
            List<AttendancePunch> nearby, ZoneId zone) {
        LocalDate localDate = at.atZone(zone).toLocalDate();
        AttendancePunch open = null;
        for (AttendancePunch p : nearby) {
            if (!p.punchedAt().isBefore(at)) break;
            if (p.punchType() == PunchType.IN) open = p;
            else if (p.punchType() == PunchType.OUT) open = null;
        }
        if (type != PunchType.IN) {
            if (open == null || open.attendanceRecordId() == null) return Optional.empty();
            return store.record(open.attendanceRecordId()).map(AttendanceRecord::workDate);
        }
        if (localDate.equals(LocalDate.MIN)) return Optional.empty();
        LocalDate prior = localDate.minusDays(1);
        var priorSchedule = schedules.get(employeeId, prior);
        if (priorSchedule.determination() == ScheduleDetermination.INDETERMINATE
                || (priorSchedule.determination() == ScheduleDetermination.EXACT
                        && !hasUniqueSchedule(employeeId, prior, zone))) return Optional.empty();
        boolean inPrior = containsScheduledInstant(employeeId, prior, at, zone);
        boolean inCurrent = containsScheduledInstant(employeeId, localDate, at, zone);
        if (inPrior && inCurrent) return Optional.empty();
        if (inPrior) return Optional.of(prior);
        return hasUniqueSchedule(employeeId, localDate, zone) ? Optional.of(localDate) : Optional.empty();
    }

    private boolean containsScheduledInstant(UUID employeeId, LocalDate date, Instant at, ZoneId zone) {
        if (!hasUniqueSchedule(employeeId, date, zone)) return false;
        var schedule = schedules.get(employeeId, date);
        try {
            Instant start = zones.uniqueInstant(schedule.intervals().getFirst().start(), zone);
            Instant end = zones.uniqueInstant(schedule.intervals().getFirst().end(), zone);
            return !at.isBefore(start) && at.isBefore(end);
        } catch (AttendanceZone.IndeterminateScheduleTime ex) {
            return false;
        }
    }

    private boolean hasUniqueSchedule(UUID employeeId, LocalDate date, ZoneId zone) {
        var schedule = schedules.get(employeeId, date);
        if (schedule.determination() != ScheduleDetermination.EXACT || schedule.intervals().size() != 1)
            return false;
        var effective = assignments.findEffectiveOn(employeeId, date);
        if (effective.size() != 1)
            return false;
        var shift = shifts.findById(effective.getFirst().shiftId());
        if (shift.isEmpty()) return false;
        try {
            Instant start = zones.uniqueInstant(schedule.intervals().getFirst().start(), zone);
            Instant end = zones.uniqueInstant(schedule.intervals().getFirst().end(), zone);
            for (var pause : breaks.findAllByShiftId(shift.get().id())) {
                var interval = ShiftTimeRules.breakInterval(shift.get(), pause, date);
                zones.uniqueInstant(interval.start(), zone);
                zones.uniqueInstant(interval.end(), zone);
            }
            return end.isAfter(start);
        } catch (AttendanceZone.IndeterminateScheduleTime | BusinessException ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponses.Punch> list(UUID employeeId, UUID recordId, Instant from, Instant to,
            PunchType type, PunchSource source, int page, int size, String sort, String direction) {
        plans.assertCanOperate();
        if (from != null && to != null && from.isAfter(to)) throw new BusinessException("from must not exceed to");
        if (recordId != null) store.record(recordId).orElseThrow(() -> new ResourceNotFoundException("Record not found: " + recordId));
        return store.punches(employeeId, recordId, from, to, type, source,
                AttendancePage.of(page, size, sort, direction, "punchedAt", SORT)).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceResponses.Punch detail(UUID id) {
        plans.assertCanOperate();
        return mapper.toResponse(store.punch(id)
                .orElseThrow(() -> new ResourceNotFoundException("Punch not found: " + id)));
    }
}
