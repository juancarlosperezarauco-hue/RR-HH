package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkShiftRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkShiftResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShiftFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkShiftService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "code", "name", "startTime", "status", "createdAt", "updatedAt");

    private final WorkShiftRepository shifts;
    private final ShiftAssignmentRepository assignments;
    private final TenantPlanEnforcementService plans;
    private final TenantShiftClock clock;
    private final SecurityContextFacade security;
    private final AuditTrailService audit;

    public WorkShiftService(WorkShiftRepository shifts, ShiftAssignmentRepository assignments,
                            TenantPlanEnforcementService plans, TenantShiftClock clock,
                            SecurityContextFacade security, AuditTrailService audit) {
        this.shifts = shifts;
        this.assignments = assignments;
        this.plans = plans;
        this.clock = clock;
        this.security = security;
        this.audit = audit;
    }

    @Transactional
    public WorkShiftResponse create(WorkShiftRequest request) {
        plans.assertCanOperate();
        WorkShift candidate = normalize(null, request, null);
        assertCodeAvailable(candidate.code(), null);
        WorkShift saved = shifts.save(candidate);
        record("CREATED", null, saved);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<WorkShiftResponse> list(String code, String name, ShiftStatus status,
                                        int page, int size, String sort, String direction) {
        plans.assertCanOperate();
        return shifts.findAll(new WorkShiftFilter(code, name, status),
                ShiftPage.of(page, size, sort, direction, "createdAt", SORT_FIELDS))
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public WorkShiftResponse detail(UUID id) {
        plans.assertCanOperate();
        return response(require(id));
    }

    @Transactional
    public WorkShiftResponse update(UUID id, WorkShiftRequest request) {
        plans.assertCanOperate();
        WorkShift before = requireLocked(id);
        WorkShift candidate = normalize(id, request, before);
        if (sameContent(before, candidate)) return response(before);
        assertCodeAvailable(candidate.code(), id);
        if (scheduleChanged(before, candidate) && assignments.existsAnyByShiftId(id)) {
            throw new BusinessException("A shift with assignment history cannot change its schedule or tolerances");
        }
        WorkShift saved = shifts.save(candidate);
        record("UPDATED", before, saved);
        return response(saved);
    }

    @Transactional
    public WorkShiftResponse activate(UUID id) {
        plans.assertCanOperate();
        WorkShift before = requireLocked(id);
        if (before.status() == ShiftStatus.ACTIVE) return response(before);
        WorkShift saved = shifts.save(withStatus(before, ShiftStatus.ACTIVE));
        record("ACTIVATED", before, saved);
        return response(saved);
    }

    @Transactional
    public WorkShiftResponse deactivate(UUID id) {
        plans.assertCanOperate();
        WorkShift before = requireLocked(id);
        if (before.status() == ShiftStatus.INACTIVE) return response(before);
        if (assignments.existsActiveCurrentOrFutureByShiftId(id, clock.today())) {
            throw new BusinessException("A shift with current or future active assignments cannot be deactivated");
        }
        WorkShift saved = shifts.save(withStatus(before, ShiftStatus.INACTIVE));
        record("DEACTIVATED", before, saved);
        return response(saved);
    }

    private WorkShift require(UUID id) {
        return shifts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Work shift not found: " + id));
    }

    private WorkShift requireLocked(UUID id) {
        return shifts.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Work shift not found: " + id));
    }

    private WorkShift normalize(UUID id, WorkShiftRequest request, WorkShift before) {
        if (request == null || request.crossesMidnight() == null
                || request.lateToleranceMinutes() == null
                || request.earlyDepartureToleranceMinutes() == null) {
            throw new BusinessException("Shift times, crossing flag and tolerances are required");
        }
        ShiftTimeRules.validateShift(request.startTime(), request.endTime(), request.crossesMidnight());
        if (request.lateToleranceMinutes() < 0 || request.earlyDepartureToleranceMinutes() < 0) {
            throw new BusinessException("Shift tolerances must be nonnegative");
        }
        return new WorkShift(id, required(request.code(), 50, "code"),
                required(request.name(), 150, "name"), optional(request.description()),
                request.startTime(), request.endTime(), request.crossesMidnight(),
                request.lateToleranceMinutes(), request.earlyDepartureToleranceMinutes(),
                before == null ? ShiftStatus.ACTIVE : before.status(),
                before == null ? actorId() : before.createdBy(),
                before == null ? null : before.createdAt(),
                before == null ? null : before.updatedAt());
    }

    private String required(String value, int max, String field) {
        if (value == null || value.isBlank()) throw new BusinessException(field + " is required");
        String normalized = value.trim();
        if (normalized.length() > max) throw new BusinessException(field + " exceeds " + max + " characters");
        return normalized;
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UUID actorId() {
        try {
            return UUID.fromString(security.getCurrentSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException("Authenticated tenant user is required");
        }
    }

    private void assertCodeAvailable(String code, UUID id) {
        if (shifts.existsByCodeOtherThan(code, id)) {
            throw new BusinessException("Work shift code already exists");
        }
    }

    private boolean sameContent(WorkShift a, WorkShift b) {
        return Objects.equals(a.code(), b.code())
                && Objects.equals(a.name(), b.name())
                && Objects.equals(a.description(), b.description())
                && !scheduleChanged(a, b);
    }

    private boolean scheduleChanged(WorkShift a, WorkShift b) {
        return !Objects.equals(a.startTime(), b.startTime())
                || !Objects.equals(a.endTime(), b.endTime())
                || a.crossesMidnight() != b.crossesMidnight()
                || a.lateToleranceMinutes() != b.lateToleranceMinutes()
                || a.earlyDepartureToleranceMinutes() != b.earlyDepartureToleranceMinutes();
    }

    private WorkShift withStatus(WorkShift before, ShiftStatus status) {
        return new WorkShift(before.id(), before.code(), before.name(), before.description(),
                before.startTime(), before.endTime(), before.crossesMidnight(),
                before.lateToleranceMinutes(), before.earlyDepartureToleranceMinutes(),
                status, before.createdBy(), before.createdAt(), before.updatedAt());
    }

    private WorkShiftResponse response(WorkShift shift) {
        return new WorkShiftResponse(shift.id(), shift.code(), shift.name(), shift.description(),
                shift.startTime(), shift.endTime(), shift.crossesMidnight(),
                shift.lateToleranceMinutes(), shift.earlyDepartureToleranceMinutes(),
                shift.status(), shift.createdAt(), shift.updatedAt());
    }

    private void record(String action, WorkShift before, WorkShift after) {
        audit.recordTenantEvent("WORK_SHIFT_" + action, "WORK_SHIFT", after.id().toString(),
                Map.of("action", action), before, after);
    }
}
