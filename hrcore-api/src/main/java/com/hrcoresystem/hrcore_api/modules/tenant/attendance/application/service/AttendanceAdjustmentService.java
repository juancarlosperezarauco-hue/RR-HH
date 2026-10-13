package com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.governance.audit.domain.model.AuditEventTypes;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceRequests;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.application.dto.AttendanceResponses;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.attendance.domain.repository.AttendanceStore;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceAdjustmentService {
    private static final Set<String> SORT = Set.of("createdAt", "updatedAt", "status", "adjustmentType");
    private static final Set<String> INVALID_CORRECTION_RESULTS = Set.of(
            "DUPLICATE_IN", "UNMATCHED_OUT", "INVALID_BREAK_START", "UNMATCHED_BREAK_END",
            "BREAK_PAY_UNKNOWN", "INVALID_WORK_DURATION");
    private final AttendanceStore store;
    private final EmployeeRepository employees;
    private final AttendanceCalculationService calculations;
    private final AttendanceActor actor;
    private final AttendanceZone zones;
    private final AttendanceMapper mapper;
    private final TenantPlanEnforcementService plans;
    private final AuditTrailService audit;
    private final ObjectMapper json;

    public AttendanceAdjustmentService(AttendanceStore store, EmployeeRepository employees,
            AttendanceCalculationService calculations, AttendanceActor actor, AttendanceZone zones,
            AttendanceMapper mapper, TenantPlanEnforcementService plans, AuditTrailService audit,
            ObjectMapper json) {
        this.store = store; this.employees = employees; this.calculations = calculations;
        this.actor = actor; this.zones = zones; this.mapper = mapper; this.plans = plans;
        this.audit = audit; this.json = json;
    }

    @Transactional
    public AttendanceResponses.Adjustment create(AttendanceRequests.Adjustment request) {
        plans.assertCanOperate();
        UUID requester = actor.id();
        AttendanceRecord found = requireRecord(request.attendanceRecordId());
        lockEmployee(found.employeeId());
        AttendanceRecord record = store.lockOrCreate(found.employeeId(), found.workDate());
        JsonNode requested = validate(request.adjustmentType(), request.requestedValues(), record);
        if (request.reason() == null || request.reason().isBlank()) throw new BusinessException("reason is required");
        if (store.adjustments(record.id()).stream().anyMatch(a -> a.status() == ReviewStatus.PENDING))
            throw new BusinessException("An attendance adjustment is already pending for this record");
        JsonNode previous = previousValues(record, request.adjustmentType(), requested);
        AttendanceAdjustment saved = store.save(new AttendanceAdjustment(null, record.id(), request.adjustmentType(),
                request.reason().trim(), previous, requested, ReviewStatus.PENDING,
                requester, null, null, null, null, null, null));
        audit.recordTenantEvent(AuditEventTypes.ATTENDANCE_ADJUSTMENT_REQUESTED, "ATTENDANCE_ADJUSTMENT",
                saved.id().toString(), Map.of("recordId", record.id().toString(), "type", saved.adjustmentType().name()),
                null, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public AttendanceResponses.Adjustment approve(UUID id, String comment) {
        return review(id, ReviewStatus.APPROVED, comment);
    }
    @Transactional
    public AttendanceResponses.Adjustment reject(UUID id, String comment) {
        return review(id, ReviewStatus.REJECTED, comment);
    }
    @Transactional
    public AttendanceResponses.Adjustment cancel(UUID id) {
        return review(id, ReviewStatus.CANCELLED, null);
    }

    private AttendanceResponses.Adjustment review(UUID id, ReviewStatus decision, String comment) {
        plans.assertCanOperate();
        UUID user = actor.id();
        AttendanceAdjustment original = store.adjustment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance adjustment not found: " + id));
        AttendanceRecord found = requireRecord(original.attendanceRecordId());
        lockEmployee(found.employeeId());
        AttendanceRecord record = store.lockOrCreate(found.employeeId(), found.workDate());
        AttendanceAdjustment pending = store.lockAdjustment(id);
        if (pending.status() != ReviewStatus.PENDING) throw new BusinessException("Adjustment is already processed");
        if (decision == ReviewStatus.CANCELLED) {
            if (!pending.requestedBy().equals(user)) throw new AccessDeniedException("Only the requester may cancel the adjustment");
        } else if (pending.requestedBy().equals(user)) {
            throw new AccessDeniedException("The requester cannot review their own adjustment");
        }
        if (decision == ReviewStatus.APPROVED) {
            JsonNode snapshot = pending.previousValues();
            JsonNode expected = snapshot.path("calculatedAt");
            JsonNode current = json.valueToTree(record.calculatedAt());
            if (!Objects.equals(expected, current)) throw new BusinessException("Attendance changed after the adjustment request");
            validate(pending.adjustmentType(), pending.requestedValues(), record);
        }
        Instant now = Instant.now();
        AttendanceAdjustment changed = new AttendanceAdjustment(pending.id(), pending.attendanceRecordId(),
                pending.adjustmentType(), pending.reason(), pending.previousValues(), pending.requestedValues(),
                decision, pending.requestedBy(), user, now, trim(comment),
                decision == ReviewStatus.APPROVED ? now : null, pending.createdAt(), pending.updatedAt());
        AttendanceAdjustment saved = store.save(changed);
        if (decision == ReviewStatus.APPROVED) {
            AttendanceRecord recalculated = calculations.recalculateLocked(record);
            if ((pending.adjustmentType() == AdjustmentType.MISSING_PUNCH
                    || pending.adjustmentType() == AdjustmentType.TIME_CORRECTION)
                    && recalculated.status() == AttendanceStatus.INCOMPLETE
                    && INVALID_CORRECTION_RESULTS.contains(
                            recalculated.calculationDetails().path("reason").asText()))
                throw new BusinessException("The approved correction creates an invalid attendance sequence");
            if ((pending.adjustmentType() == AdjustmentType.STATUS_CORRECTION
                    || pending.adjustmentType() == AdjustmentType.OTHER)
                    && recalculated.status() != AttendanceStatus.valueOf(
                            pending.requestedValues().path("status").asText()))
                throw new BusinessException("The requested status cannot override incomplete attendance evidence");
            if (recalculated != record)
                audit.recordTenantEvent(AuditEventTypes.ATTENDANCE_ADJUSTMENT_APPLIED,
                        "ATTENDANCE_ADJUSTMENT", id.toString(),
                        Map.of("recordId", record.id().toString()),
                        mapper.toResponse(record), mapper.toResponse(recalculated));
        }
        String eventType = switch (decision) {
            case APPROVED -> AuditEventTypes.ATTENDANCE_ADJUSTMENT_APPROVED;
            case REJECTED -> AuditEventTypes.ATTENDANCE_ADJUSTMENT_REJECTED;
            case CANCELLED -> AuditEventTypes.ATTENDANCE_ADJUSTMENT_CANCELLED;
            default -> throw new IllegalStateException("Unsupported review decision");
        };
        audit.recordTenantEvent(eventType, "ATTENDANCE_ADJUSTMENT", id.toString(),
                Map.of("recordId", record.id().toString()), mapper.toResponse(pending), mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    private JsonNode validate(AdjustmentType type, JsonNode values, AttendanceRecord record) {
        if (type == null || values == null || !values.isObject())
            throw new BusinessException("A typed requestedValues object is required");
        Set<String> keys = new HashSet<>();
        values.fieldNames().forEachRemaining(keys::add);
        try {
            switch (type) {
                case MISSING_PUNCH -> {
                    requireKeys(keys, Set.of("punchType", "punchedAt"));
                    PunchType.valueOf(values.path("punchType").asText());
                    checkDate(Instant.parse(values.path("punchedAt").asText()), record);
                }
                case TIME_CORRECTION -> {
                    requireKeys(keys, Set.of("punchId", "punchedAt"));
                    UUID punchId = UUID.fromString(values.path("punchId").asText());
                    AttendancePunch target = store.punch(punchId).orElseThrow();
                    if (!record.id().equals(target.attendanceRecordId()) || !record.employeeId().equals(target.employeeId()))
                        throw new BusinessException("Punch does not belong to this attendance record");
                    checkDate(Instant.parse(values.path("punchedAt").asText()), record);
                }
                case STATUS_CORRECTION -> {
                    requireKeys(keys, Set.of("status"));
                    AttendanceStatus.valueOf(values.path("status").asText());
                }
                case OTHER -> {
                    requireKeys(keys, Set.of("status"));
                    if (!"JUSTIFIED".equals(values.path("status").asText()))
                        throw new BusinessException("OTHER may only request JUSTIFIED");
                }
            }
        } catch (IllegalArgumentException | NoSuchElementException ex) {
            throw new BusinessException("Invalid requestedValues for " + type);
        }
        return values.deepCopy();
    }

    private void requireKeys(Set<String> actual, Set<String> expected) {
        if (!actual.equals(expected)) throw new BusinessException("requestedValues must contain exactly " + expected);
    }

    private void checkDate(Instant at, AttendanceRecord record) {
        ZoneId zone = zones.required();
        LocalDate date = at.atZone(zone).toLocalDate();
        if (!date.equals(record.workDate()) && !date.equals(record.workDate().plusDays(1)))
            throw new BusinessException("Corrected punch must fall on the work date or following date");
    }

    private JsonNode previousValues(AttendanceRecord record, AdjustmentType type, JsonNode requested) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("calculatedAt", record.calculatedAt());
        values.put("status", record.status());
        values.put("actualFirstInAt", record.actualFirstInAt());
        values.put("actualLastOutAt", record.actualLastOutAt());
        values.put("workedMinutes", record.workedMinutes());
        if (type == AdjustmentType.TIME_CORRECTION) {
            AttendancePunch original = store.punch(UUID.fromString(requested.path("punchId").asText())).orElseThrow();
            values.put("punchId", original.id()); values.put("punchedAt", original.punchedAt());
        }
        return json.valueToTree(values);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponses.Adjustment> list(UUID recordId, UUID requestedBy, AdjustmentType type,
            ReviewStatus status, int page, int size, String sort, String direction) {
        plans.assertCanOperate();
        if (recordId != null) requireRecord(recordId);
        return store.adjustments(recordId, requestedBy, type, status,
                AttendancePage.of(page, size, sort, direction, "createdAt", SORT)).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceResponses.Adjustment detail(UUID id) {
        plans.assertCanOperate();
        return mapper.toResponse(store.adjustment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance adjustment not found: " + id)));
    }

    private AttendanceRecord requireRecord(UUID id) {
        return store.record(id).orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));
    }
    private void lockEmployee(UUID id) {
        employees.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
