package com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.CreateShiftAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.UpdateShiftAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.EmployeeShiftAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class EmployeeShiftAssignmentService {
    private static final String RESOURCE = "EMPLOYEE_SHIFT_ASSIGNMENT";
    private final ShiftAssignmentRepository assignments;
    private final WorkShiftRepository shifts;
    private final EmployeeRepository employees;
    private final TenantPlanEnforcementService plans;
    private final TenantShiftClock clock;
    private final SecurityContextFacade security;
    private final AuditTrailService audit;
    private final ShiftAssignmentConflictPolicy conflicts;

    public EmployeeShiftAssignmentService(ShiftAssignmentRepository assignments, WorkShiftRepository shifts,
            EmployeeRepository employees, TenantPlanEnforcementService plans, TenantShiftClock clock,
            SecurityContextFacade security, AuditTrailService audit, ShiftAssignmentConflictPolicy conflicts) {
        this.assignments = assignments; this.shifts = shifts; this.employees = employees;
        this.plans = plans; this.clock = clock; this.security = security;
        this.audit = audit; this.conflicts = conflicts;
    }

    @Transactional
    public ShiftAssignmentResponse create(CreateShiftAssignmentRequest request) {
        plans.assertCanOperate();
        LocalDate today = clock.today();
        if (request == null || request.employeeId() == null || request.shiftId() == null)
            throw new BusinessException("employeeId and shiftId are required");
        Employee employee = lockEmployee(request.employeeId());
        requireActiveEmployee(employee);
        List<Short> weekdays = weekdays(request.weekdays());
        validateDates(request.startDate(), request.endDate(), employee, today, true);
        List<EmployeeShiftAssignment> possible = assignments.findActiveOverlapping(employee.id(), null,
                request.startDate(), request.endDate());
        Map<UUID, WorkShift> lockedShifts = lockShifts(possible, request.shiftId(), null);
        WorkShift shift = activeShift(lockedShifts, request.shiftId());
        EmployeeShiftAssignment draft = new EmployeeShiftAssignment(null, employee.id(), shift.id(),
                request.startDate(), request.endDate(), weekdays, ShiftAssignmentStatus.ACTIVE,
                actorId(), null, null);
        assertNoConflict(draft, shift, possible, lockedShifts);
        EmployeeShiftAssignment saved = assignments.save(draft);
        audit("CREATED", saved.id(), List.of("employeeId", "shiftId", "startDate", "endDate", "weekdays"));
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<ShiftAssignmentResponse> list(UUID employeeId, UUID shiftId,
            ShiftAssignmentStatus status, Pageable pageable) {
        plans.assertCanOperate();
        return assignments.findAll(employeeId, shiftId, status, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public ShiftAssignmentResponse get(UUID id) {
        plans.assertCanOperate();
        return response(require(id));
    }

    @Transactional
    public ShiftAssignmentResponse update(UUID id, UpdateShiftAssignmentRequest request) {
        plans.assertCanOperate();
        LocalDate today = clock.today();
        if (request == null || request.shiftId() == null)
            throw new BusinessException("shiftId is required");
        EmployeeShiftAssignment before = lockOwnerAndReload(id);
        if (before.status() != ShiftAssignmentStatus.ACTIVE || !before.startDate().isAfter(today))
            throw new BusinessException("Only future active assignments may be updated");
        Employee employee = employees.findById(before.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + before.employeeId()));
        requireActiveEmployee(employee);
        List<Short> weekdays = weekdays(request.weekdays());
        validateDates(request.startDate(), request.endDate(), employee, today, false);
        List<EmployeeShiftAssignment> possible = assignments.findActiveOverlapping(employee.id(), id,
                request.startDate(), request.endDate());
        Map<UUID, WorkShift> lockedShifts = lockShifts(possible, request.shiftId(), before.shiftId());
        WorkShift shift = activeShift(lockedShifts, request.shiftId());
        EmployeeShiftAssignment draft = new EmployeeShiftAssignment(id, before.employeeId(), request.shiftId(),
                request.startDate(), request.endDate(), weekdays, ShiftAssignmentStatus.ACTIVE,
                before.createdBy(), before.createdAt(), before.updatedAt());
        List<String> changed = changed(before, draft);
        if (changed.isEmpty()) return response(before);
        assertNoConflict(draft, shift, possible, lockedShifts);
        EmployeeShiftAssignment saved = assignments.save(draft);
        audit("UPDATED", saved.id(), changed);
        return response(saved);
    }

    @Transactional
    public ShiftAssignmentResponse cancel(UUID id) {
        plans.assertCanOperate();
        LocalDate today = clock.today();
        EmployeeShiftAssignment before = lockOwnerAndReload(id);
        lockShifts(List.of(), before.shiftId(), null);
        if (!before.startDate().isAfter(today))
            throw new BusinessException("Only assignments starting in the future may be cancelled");
        if (before.status() == ShiftAssignmentStatus.CANCELLED) return response(before);
        if (before.status() != ShiftAssignmentStatus.ACTIVE)
            throw new BusinessException("Only active assignments may be cancelled");
        EmployeeShiftAssignment saved = assignments.save(withStatus(before,
                ShiftAssignmentStatus.CANCELLED, before.endDate()));
        audit("CANCELLED", saved.id(), List.of("status"));
        return response(saved);
    }

    @Transactional
    public ShiftAssignmentResponse close(UUID id) {
        plans.assertCanOperate();
        LocalDate today = clock.today();
        EmployeeShiftAssignment before = lockOwnerAndReload(id);
        lockShifts(List.of(), before.shiftId(), null);
        if (before.status() == ShiftAssignmentStatus.INACTIVE) return response(before);
        if (before.status() != ShiftAssignmentStatus.ACTIVE || before.startDate().isAfter(today))
            throw new BusinessException("Only started active assignments may be closed");
        LocalDate endDate = before.endDate() != null && before.endDate().isBefore(today)
                ? before.endDate() : today;
        EmployeeShiftAssignment saved = assignments.save(withStatus(before,
                ShiftAssignmentStatus.INACTIVE, endDate));
        audit("CLOSED", saved.id(), List.of("status", "endDate"));
        return response(saved);
    }

    private EmployeeShiftAssignment lockOwnerAndReload(UUID id) {
        UUID employeeId = assignments.findEmployeeId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift assignment not found: " + id));
        lockEmployee(employeeId);
        return require(id);
    }

    private Employee lockEmployee(UUID id) {
        return employees.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private EmployeeShiftAssignment require(UUID id) {
        return assignments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift assignment not found: " + id));
    }

    private void requireActiveEmployee(Employee employee) {
        if (employee.employmentStatus() != EmploymentStatus.ACTIVE)
            throw new BusinessException("Employee must be active");
    }

    private WorkShift activeShift(Map<UUID, WorkShift> locked, UUID id) {
        WorkShift shift = locked.get(id);
        if (shift == null) throw new ResourceNotFoundException("Work shift not found: " + id);
        if (shift.status() != ShiftStatus.ACTIVE)
            throw new BusinessException("Work shift must be active");
        return shift;
    }

    private Map<UUID, WorkShift> lockShifts(List<EmployeeShiftAssignment> possible,
            UUID first, UUID second) {
        Set<UUID> ids = new HashSet<>();
        if (first != null) ids.add(first);
        if (second != null) ids.add(second);
        for (EmployeeShiftAssignment assignment : possible) ids.add(assignment.shiftId());
        Map<UUID, WorkShift> result = new HashMap<>();
        ids.stream().sorted(Comparator.naturalOrder()).forEach(id -> result.put(id,
                shifts.lockById(id).orElseThrow(() ->
                        new ResourceNotFoundException("Work shift not found: " + id))));
        return result;
    }

    private void assertNoConflict(EmployeeShiftAssignment draft, WorkShift shift,
            List<EmployeeShiftAssignment> possible, Map<UUID, WorkShift> lockedShifts) {
        for (EmployeeShiftAssignment other : possible) {
            if (conflicts.conflicts(draft, shift, other, lockedShifts.get(other.shiftId())))
                throw new BusinessException("Shift assignment overlaps another active assignment");
        }
    }

    private List<Short> weekdays(List<Short> values) {
        if (values == null || values.isEmpty() || values.size() > 7)
            throw new BusinessException("weekdays must contain 1 to 7 ISO days");
        Set<Short> unique = new HashSet<>();
        for (Short value : values) {
            if (value == null || value < 1 || value > 7)
                throw new BusinessException("weekdays must use ISO values 1 to 7");
            if (!unique.add(value)) throw new BusinessException("weekdays cannot contain duplicates");
        }
        return unique.stream().sorted().toList();
    }

    private void validateDates(LocalDate startDate, LocalDate endDate, Employee employee,
            LocalDate today, boolean allowToday) {
        if (startDate == null) throw new BusinessException("startDate is required");
        if (startDate.isBefore(today) || (!allowToday && startDate.equals(today)))
            throw new BusinessException(allowToday ? "startDate cannot precede today"
                    : "Updated assignment must start after today");
        if (employee.hireDate() != null && startDate.isBefore(employee.hireDate()))
            throw new BusinessException("startDate cannot precede employee hireDate");
        if (endDate != null && endDate.isBefore(startDate))
            throw new BusinessException("endDate cannot precede startDate");
    }

    private EmployeeShiftAssignment withStatus(EmployeeShiftAssignment before,
            ShiftAssignmentStatus status, LocalDate endDate) {
        return new EmployeeShiftAssignment(before.id(), before.employeeId(), before.shiftId(),
                before.startDate(), endDate, before.weekdays(), status,
                before.createdBy(), before.createdAt(), before.updatedAt());
    }

    private List<String> changed(EmployeeShiftAssignment a, EmployeeShiftAssignment b) {
        List<String> fields = new ArrayList<>();
        if (!Objects.equals(a.shiftId(), b.shiftId())) fields.add("shiftId");
        if (!Objects.equals(a.startDate(), b.startDate())) fields.add("startDate");
        if (!Objects.equals(a.endDate(), b.endDate())) fields.add("endDate");
        if (!Objects.equals(a.weekdays(), b.weekdays())) fields.add("weekdays");
        return fields;
    }

    private UUID actorId() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }

    private void audit(String action, UUID id, List<String> changedFields) {
        audit.recordTenantEvent("SHIFT_ASSIGNMENT_" + action, RESOURCE, id.toString(),
                Map.of("action", action, "changedFields", changedFields));
    }

    private ShiftAssignmentResponse response(EmployeeShiftAssignment assignment) {
        return new ShiftAssignmentResponse(assignment.id(), assignment.employeeId(), assignment.shiftId(),
                assignment.startDate(), assignment.endDate(), assignment.weekdays(), assignment.status(),
                assignment.createdAt(), assignment.updatedAt());
    }
}
