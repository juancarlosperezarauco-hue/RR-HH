package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.application.dto.NotificationPublishRequest;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationCategory;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationPriority;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.model.NotificationType;
import com.hrcoresystem.hrcore_api.modules.governance.notifications.domain.port.NotificationPublisherPort;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AttendanceOvertimeService {
    private static final Set<String> SORT = Set.of("createdAt", "updatedAt", "status", "requestedMinutes");
    private final AttendanceStore store;
    private final EmployeeRepository employees;
    private final AttendanceActor actor;
    private final AttendanceMapper mapper;
    private final TenantPlanEnforcementService plans;
    private final AuditTrailService audit;
    private final NotificationPublisherPort notificationPublisher;

    @Autowired
    public AttendanceOvertimeService(AttendanceStore store, EmployeeRepository employees, AttendanceActor actor,
            AttendanceMapper mapper, TenantPlanEnforcementService plans, AuditTrailService audit,
            NotificationPublisherPort notificationPublisher) {
        this.store = store; this.employees = employees; this.actor = actor;
        this.mapper = mapper; this.plans = plans; this.audit = audit; this.notificationPublisher = notificationPublisher;
    }
    public AttendanceOvertimeService(AttendanceStore store, EmployeeRepository employees, AttendanceActor actor,
            AttendanceMapper mapper, TenantPlanEnforcementService plans, AuditTrailService audit) {
        this(store, employees, actor, mapper, plans, audit, null);
    }

    @Transactional
    public AttendanceResponses.Overtime create(AttendanceRequests.Overtime request) {
        plans.assertCanOperate();
        UUID requester = actor.id();
        if (request.requestedMinutes() == null || request.requestedMinutes() <= 0)
            throw new BusinessException("requestedMinutes must be positive");
        AttendanceRecord found = requireRecord(request.attendanceRecordId());
        lockEmployee(found.employeeId());
        AttendanceRecord record = store.lockOrCreate(found.employeeId(), found.workDate());
        int eligible = eligibleOvertimeMinutes(record);
        if (eligible <= 0 || request.requestedMinutes() > eligible)
            throw new BusinessException("Los minutos solicitados exceden las horas extras verificables (máximo " + eligible + ")");
        if (store.overtimeForRecord(record.id()).stream().anyMatch(value ->
                value.status() == ReviewStatus.PENDING || value.status() == ReviewStatus.APPROVED))
            throw new BusinessException("An active overtime request already exists for this attendance record");
        AttendanceOvertime saved = store.save(new AttendanceOvertime(null, record.id(),
                request.requestedMinutes(), null, trim(request.reason()), ReviewStatus.PENDING,
                null, null, requester, null, null));
        audit.recordTenantEvent(AuditEventTypes.ATTENDANCE_OVERTIME_REQUESTED, "ATTENDANCE_OVERTIME",
                saved.id().toString(), Map.of("recordId", record.id().toString(),
                        "requestedMinutes", saved.requestedMinutes()), null, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public AttendanceResponses.Overtime approve(UUID id, AttendanceRequests.OvertimeApproval request) {
        return review(id, ReviewStatus.APPROVED, request.approvedMinutes(), request.comment());
    }
    @Transactional
    public AttendanceResponses.Overtime reject(UUID id, String comment) {
        return review(id, ReviewStatus.REJECTED, null, comment);
    }
    @Transactional
    public AttendanceResponses.Overtime cancel(UUID id) {
        return review(id, ReviewStatus.CANCELLED, null, null);
    }

    private AttendanceResponses.Overtime review(UUID id, ReviewStatus decision,
            Integer approvedMinutes, String comment) {
        plans.assertCanOperate();
        UUID user = actor.id();
        AttendanceOvertime original = store.overtime(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance overtime not found: " + id));
        AttendanceRecord found = requireRecord(original.attendanceRecordId());
        lockEmployee(found.employeeId());
        store.lockOrCreate(found.employeeId(), found.workDate());
        AttendanceOvertime pending = store.lockOvertime(id);
        if (pending.status() != ReviewStatus.PENDING) throw new BusinessException("Overtime request is already processed");
        UUID requester = pending.requestedBy();
        if (requester == null) {
            List<UUID> actors = store.overtimeRequestActors(id);
            if (actors.size() != 1) throw new BusinessException("La solicitud no tiene un solicitante persistido");
            requester = actors.getFirst();
        }
        if (decision == ReviewStatus.CANCELLED) {
            if (!requester.equals(user)) throw new AccessDeniedException("Only the requester may cancel overtime");
        } else if (requester.equals(user)) {
            throw new AccessDeniedException("The requester cannot review their own overtime");
        }
        if (decision == ReviewStatus.APPROVED && (approvedMinutes == null || approvedMinutes < 0
                || approvedMinutes > pending.requestedMinutes()))
            throw new BusinessException("approvedMinutes must be between zero and requestedMinutes");
        int eligible = eligibleOvertimeMinutes(store.record(pending.attendanceRecordId()).orElse(found));
        if (decision == ReviewStatus.APPROVED && (eligible <= 0 || approvedMinutes > eligible))
            throw new BusinessException("Los minutos aprobados exceden las horas extras verificables (máximo " + eligible + ")");
        Instant now = Instant.now();
        AttendanceOvertime saved = store.save(new AttendanceOvertime(pending.id(), pending.attendanceRecordId(),
                pending.requestedMinutes(), decision == ReviewStatus.APPROVED ? approvedMinutes : null,
                pending.reason(), decision, decision == ReviewStatus.APPROVED ? user : null,
                decision == ReviewStatus.APPROVED ? now : null, pending.requestedBy(), pending.createdAt(), pending.updatedAt()));
        String eventType = switch (decision) {
            case APPROVED -> AuditEventTypes.ATTENDANCE_OVERTIME_APPROVED;
            case REJECTED -> AuditEventTypes.ATTENDANCE_OVERTIME_REJECTED;
            case CANCELLED -> AuditEventTypes.ATTENDANCE_OVERTIME_CANCELLED;
            default -> throw new IllegalStateException("Unsupported review decision");
        };
        audit.recordTenantEvent(eventType, "ATTENDANCE_OVERTIME", id.toString(),
                Map.of("recordId", pending.attendanceRecordId().toString(),
                        "requesterId", requester.toString(), "comment", comment == null ? "" : comment),
                mapper.toResponse(pending), mapper.toResponse(saved));
        if (decision == ReviewStatus.APPROVED || decision == ReviewStatus.REJECTED) {
            publishDecision(requester, decision, id);
        }
        return mapper.toResponse(saved);
    }

    private void publishDecision(UUID userId, ReviewStatus decision, UUID id) {
        if (notificationPublisher == null || userId == null) return;
        NotificationType type = NotificationType.OVERTIME_REVIEW_REQUIRED;
        String title = decision == ReviewStatus.APPROVED ? "Horas extra aprobadas" : "Horas extra rechazadas";
        String body = decision == ReviewStatus.APPROVED
                ? "Tu solicitud de horas extra fue aprobada." : "Tu solicitud de horas extra fue rechazada.";
        try {
            notifyAfterCommit(new NotificationPublishRequest(userId, type, NotificationCategory.SYSTEM,
                    NotificationPriority.NORMAL, title, body, null, null,
                    "/dashboard/attendance/overtime?id=" + id, null));
        } catch (Exception ignored) { }
    }

    private void notifyAfterCommit(NotificationPublishRequest payload) {
        if (notificationPublisher == null) return;
        Runnable publish = () -> {
            try { notificationPublisher.publish(payload); } catch (Exception ignored) { }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { publish.run(); }
        });
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponses.Overtime> list(UUID recordId, ReviewStatus status, LocalDate from, LocalDate to,
            int page, int size, String sort, String direction) {
        plans.assertCanOperate();
        if (from != null && to != null && from.isAfter(to)) throw new BusinessException("from must not exceed to");
        if (recordId != null) requireRecord(recordId);
        return store.overtime(recordId, status, from, to,
                AttendancePage.of(page, size, sort, direction, "createdAt", SORT)).map(mapper::toResponse);
    }
    @Transactional(readOnly = true)
    public Page<AttendanceResponses.Overtime> listOwn(UUID requesterId, ReviewStatus status, int page, int size) {
        plans.assertCanOperate();
        return store.overtimeByRequester(requesterId, status, null, null,
                AttendancePage.of(page, size, "createdAt", "desc", "createdAt", SORT)).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceResponses.Overtime detail(UUID id) {
        plans.assertCanOperate();
        return mapper.toResponse(store.overtime(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance overtime not found: " + id)));
    }

    private AttendanceRecord requireRecord(UUID id) {
        return store.record(id).orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));
    }
    private void lockEmployee(UUID id) {
        employees.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private int eligibleOvertimeMinutes(AttendanceRecord record) {
        if (record == null || record.expectedStartAt() == null || record.expectedEndAt() == null
                || record.actualFirstInAt() == null || record.actualLastOutAt() == null
                || record.status() == AttendanceStatus.INCOMPLETE || record.status() == AttendanceStatus.ABSENT
                || record.status() == AttendanceStatus.PENDING || record.status() == AttendanceStatus.DAY_OFF)
            return 0;
        long scheduled = Duration.between(record.expectedStartAt(), record.expectedEndAt()).toMinutes();
        if (record.calculationDetails() != null && record.calculationDetails().path("scheduledBreaks").isArray()) {
            for (var pause : record.calculationDetails().path("scheduledBreaks")) {
                if (!pause.path("paid").asBoolean(true)) {
                    String start = pause.path("start").asText(null), end = pause.path("end").asText(null);
                    try { if (start != null && end != null) scheduled -= Duration.between(Instant.parse(start), Instant.parse(end)).toMinutes(); }
                    catch (RuntimeException ignored) { return 0; }
                }
            }
        }
        return Math.max(0, record.workedMinutes() - (int) Math.max(0, scheduled));
    }
}
