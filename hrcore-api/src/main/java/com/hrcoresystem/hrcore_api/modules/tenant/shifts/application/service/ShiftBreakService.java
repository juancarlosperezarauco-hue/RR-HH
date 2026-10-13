package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftBreakRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftBreakResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftBreak;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftBreakRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ShiftBreakService {
    private static final Set<String> SORT_FIELDS = Set.of("name", "startTime", "createdAt");
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2000, 1, 1);

    private final WorkShiftRepository shifts;
    private final ShiftBreakRepository breaks;
    private final ShiftAssignmentRepository assignments;
    private final TenantPlanEnforcementService plans;
    private final AuditTrailService audit;

    public ShiftBreakService(WorkShiftRepository shifts, ShiftBreakRepository breaks,
                             ShiftAssignmentRepository assignments,
                             TenantPlanEnforcementService plans, AuditTrailService audit) {
        this.shifts = shifts;
        this.breaks = breaks;
        this.assignments = assignments;
        this.plans = plans;
        this.audit = audit;
    }

    @Transactional
    public ShiftBreakResponse create(UUID shiftId, ShiftBreakRequest request) {
        plans.assertCanOperate();
        WorkShift shift = requireLockedShift(shiftId);
        if (assignments.existsAnyByShiftId(shiftId)) {
            throw new BusinessException("A shift with assignment history cannot add breaks");
        }
        ShiftBreak candidate = normalize(null, shiftId, request, null);
        validatePlacement(shift, candidate, null);
        ShiftBreak saved = breaks.save(candidate);
        record("CREATED", null, saved);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShiftBreakResponse> list(UUID shiftId, int page, int size,
                                         String sort, String direction) {
        plans.assertCanOperate();
        requireShift(shiftId);
        return breaks.findAllByShiftId(shiftId,
                ShiftPage.of(page, size, sort, direction, "startTime", SORT_FIELDS))
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public ShiftBreakResponse detail(UUID shiftId, UUID breakId) {
        plans.assertCanOperate();
        requireShift(shiftId);
        return response(requireBreak(shiftId, breakId));
    }

    @Transactional
    public ShiftBreakResponse update(UUID shiftId, UUID breakId, ShiftBreakRequest request) {
        plans.assertCanOperate();
        WorkShift shift = requireLockedShift(shiftId);
        ShiftBreak before = requireBreak(shiftId, breakId);
        ShiftBreak candidate = normalize(breakId, shiftId, request, before);
        if (sameContent(before, candidate)) return response(before);
        if (substantialChange(before, candidate) && assignments.existsAnyByShiftId(shiftId)) {
            throw new BusinessException("A shift with assignment history cannot change break timing or pay");
        }
        validatePlacement(shift, candidate, breakId);
        ShiftBreak saved = breaks.save(candidate);
        record("UPDATED", before, saved);
        return response(saved);
    }

    @Transactional
    public void delete(UUID shiftId, UUID breakId) {
        plans.assertCanOperate();
        requireLockedShift(shiftId);
        ShiftBreak before = requireBreak(shiftId, breakId);
        if (assignments.existsAnyByShiftId(shiftId)) {
            throw new BusinessException("A shift with assignment history cannot remove breaks");
        }
        breaks.delete(before);
        record("DELETED", before, null);
    }

    private WorkShift requireShift(UUID id) {
        return shifts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Work shift not found: " + id));
    }

    private WorkShift requireLockedShift(UUID id) {
        return shifts.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Work shift not found: " + id));
    }

    private ShiftBreak requireBreak(UUID shiftId, UUID breakId) {
        return breaks.findByIdAndShiftId(breakId, shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift break not found: " + breakId));
    }

    private ShiftBreak normalize(UUID id, UUID shiftId, ShiftBreakRequest request, ShiftBreak before) {
        if (request == null || request.startTime() == null || request.endTime() == null
                || request.durationMinutes() == null || request.paid() == null) {
            throw new BusinessException("Break time, durationMinutes and paid are required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException("Break name is required");
        }
        String name = request.name().trim();
        if (name.length() > 100) throw new BusinessException("Break name exceeds 100 characters");
        if (request.durationMinutes() < 1 || request.durationMinutes() > 1440) {
            throw new BusinessException("Break durationMinutes must be between 1 and 1440");
        }
        return new ShiftBreak(id, shiftId, name, request.startTime(), request.endTime(),
                request.durationMinutes(), request.paid(),
                before == null ? null : before.createdAt());
    }

    private void validatePlacement(WorkShift shift, ShiftBreak candidate, UUID excludingId) {
        ShiftTimeRules.LocalInterval proposed = ShiftTimeRules.breakInterval(shift, candidate, REFERENCE_DATE);
        for (ShiftBreak other : breaks.findAllByShiftId(shift.id())) {
            if (other.id().equals(excludingId)) continue;
            ShiftTimeRules.LocalInterval occupied = ShiftTimeRules.breakInterval(shift, other, REFERENCE_DATE);
            if (proposed.overlaps(occupied)) {
                throw new BusinessException("Shift breaks cannot overlap");
            }
        }
    }

    private boolean substantialChange(ShiftBreak before, ShiftBreak candidate) {
        return !Objects.equals(before.startTime(), candidate.startTime())
                || !Objects.equals(before.endTime(), candidate.endTime())
                || before.durationMinutes() != candidate.durationMinutes()
                || before.paid() != candidate.paid();
    }

    private boolean sameContent(ShiftBreak before, ShiftBreak candidate) {
        return Objects.equals(before.name(), candidate.name())
                && !substantialChange(before, candidate);
    }

    private ShiftBreakResponse response(ShiftBreak shiftBreak) {
        return new ShiftBreakResponse(shiftBreak.id(), shiftBreak.shiftId(), shiftBreak.name(),
                shiftBreak.startTime(), shiftBreak.endTime(), shiftBreak.durationMinutes(),
                shiftBreak.paid(), shiftBreak.createdAt());
    }

    private void record(String action, ShiftBreak before, ShiftBreak after) {
        ShiftBreak subject = after == null ? before : after;
        audit.recordTenantEvent("SHIFT_BREAK_" + action, "SHIFT_BREAK", subject.id().toString(),
                Map.of("action", action, "shiftId", subject.shiftId().toString()), before, after);
    }
}
