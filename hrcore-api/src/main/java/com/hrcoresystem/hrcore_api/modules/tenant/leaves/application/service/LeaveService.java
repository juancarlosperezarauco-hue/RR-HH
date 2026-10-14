package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.model.TenantSettingKeys;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.domain.repository.TenantSettingRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.dto.LeaveDtos.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MAX = new BigDecimal("999999.99");
    private final LeaveStore store;
    private final EmployeeRepository employees;
    private final LeaveDayCalculator calculator;
    private final LeaveAttachmentService attachments;
    private final TenantSettingRepository settings;
    private final TenantPlanEnforcementService plans;
    private final SecurityContextFacade security;
    private final AuditTrailService audit;
    private final LeaveAttendanceSync attendanceSync;

    @Autowired
    public LeaveService(LeaveStore store, EmployeeRepository employees, LeaveDayCalculator calculator,
            LeaveAttachmentService attachments,
            TenantSettingRepository settings,
            TenantPlanEnforcementService plans, SecurityContextFacade security,
            AuditTrailService audit, LeaveAttendanceSync attendanceSync) {
        this.store = store; this.employees = employees; this.calculator = calculator;
        this.attachments = attachments;
        this.settings = settings; this.plans = plans; this.security = security; this.audit = audit;
        this.attendanceSync = attendanceSync;
    }

    /** Constructor de compatibilidad para pruebas unitarias aisladas de HU18. */
    public LeaveService(LeaveStore store, EmployeeRepository employees, LeaveDayCalculator calculator,
            LeaveAttachmentService attachments, TenantSettingRepository settings,
            TenantPlanEnforcementService plans, SecurityContextFacade security, AuditTrailService audit) {
        this(store, employees, calculator, attachments, settings, plans, security, audit,
                (employeeId, startDate, endDate) -> { });
    }

    @Transactional public TypeResponse createType(TypeInput input) {
        operational();
        String code = required(input.code(), 50, "code"), name = required(input.name(), 150, "name");
        if (store.typeCodeExists(code, null)) throw new BusinessException("Leave type code already exists");
        LeaveType saved = store.save(new LeaveType(UUID.randomUUID(), code, name, input.description(),
                required(input.paid()), required(input.requiresApproval()),
                required(input.requiresAttachment()), required(input.deductsBalance()),
                TypeStatus.ACTIVE, null, null));
        event("LEAVE_TYPE_CREATED", "LEAVE_TYPE", saved.id(), Map.of("code", code));
        return type(saved);
    }

    @Transactional(readOnly = true) public Page<TypeResponse> types(String code, String name,
            TypeStatus status, Pageable page) {
        operational(); return store.types(code, name, status, page).map(this::type);
    }
    @Transactional(readOnly = true) public TypeResponse type(UUID id) { operational(); return type(requireType(id)); }

    @Transactional public TypeResponse updateType(UUID id, TypeInput input) {
        operational(); LeaveType old = lockedType(id);
        String code = required(input.code(), 50, "code"), name = required(input.name(), 150, "name");
        if (store.typeCodeExists(code, id)) throw new BusinessException("Leave type code already exists");
        boolean paid = required(input.paid()), approval = required(input.requiresApproval()),
                attachment = required(input.requiresAttachment()), deduct = required(input.deductsBalance());
        if (store.typeHasRelevantRequests(id) && (old.paid() != paid || old.requiresApproval() != approval
                || old.requiresAttachment() != attachment || old.deductsBalance() != deduct))
            throw new BusinessException("Critical policy of a used leave type cannot change");
        LeaveType saved = store.save(new LeaveType(id, code, name, input.description(), paid, approval,
                attachment, deduct, old.status(), old.createdAt(), old.updatedAt()));
        event("LEAVE_TYPE_UPDATED", "LEAVE_TYPE", id, Map.of("code", code)); return type(saved);
    }

    @Transactional public TypeResponse typeStatus(UUID id, TypeStatus target) {
        operational(); LeaveType old = lockedType(id);
        if (old.status() == target) return type(old);
        LeaveType saved = store.save(new LeaveType(id, old.code(), old.name(), old.description(), old.paid(),
                old.requiresApproval(), old.requiresAttachment(), old.deductsBalance(), target,
                old.createdAt(), old.updatedAt()));
        event(target == TypeStatus.ACTIVE ? "LEAVE_TYPE_ACTIVATED" : "LEAVE_TYPE_DEACTIVATED",
                "LEAVE_TYPE", id, Map.of()); return type(saved);
    }

    @Transactional public BalanceResponse createBalance(BalanceInput input) {
        operational(); UUID employee = Objects.requireNonNull(input.employeeId(), "employeeId");
        actor();
        UUID type = Objects.requireNonNull(input.leaveTypeId(), "leaveTypeId");
        validateYear(input.balanceYear()); BigDecimal opening = days(input.openingDays(), false);
        lockEmployee(employee); requireType(type);
        if (store.balanceExists(employee, type, input.balanceYear()))
            throw new BusinessException("Annual leave balance already exists");
        LeaveBalance saved = store.save(new LeaveBalance(UUID.randomUUID(), employee, type,
                input.balanceYear(), opening, ZERO, ZERO, ZERO, null));
        event("LEAVE_BALANCE_CREATED", "LEAVE_BALANCE", saved.id(),
                Map.of("employeeId", employee, "year", input.balanceYear(), "openingDays", opening));
        return balance(saved);
    }

    @Transactional(readOnly = true) public Page<BalanceResponse> balances(UUID employee, UUID type,
            Integer year, Pageable page) {
        operational(); if (year != null) validateYear(year);
        Page<LeaveBalance> found = store.balances(employee, type, year, page);
        Map<UUID, BigDecimal> pending = store.pendingDaysForBalances(found.getContent());
        return found.map(v -> balance(v, pending.getOrDefault(v.id(), ZERO)));
    }
    @Transactional(readOnly = true) public BalanceResponse balance(UUID id) {
        operational(); return balance(requireBalance(id));
    }

    @Transactional public BalanceResponse adjustBalance(UUID id, BalanceAdjustment input) {
        operational(); LeaveBalance observed = requireBalance(id);
        UUID who = actor();
        lockEmployee(observed.employeeId());
        LeaveBalance old = store.lockedBalance(observed.employeeId(), observed.leaveTypeId(), observed.balanceYear())
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));
        BigDecimal delta = days(input.deltaDays(), true);
        if (delta.signum() == 0) throw new BusinessException("Adjustment must not be zero");
        String reason = required(input.reason(), 0, "reason");
        BigDecimal adjusted = old.adjustedDays().add(delta), available = old.available().add(delta);
        if (adjusted.abs().compareTo(MAX) > 0 || available.signum() < 0
                || available.subtract(store.pendingDays(old.employeeId(), old.leaveTypeId(), old.balanceYear())).signum() < 0)
            throw new BusinessException("Adjustment would overdraw the balance or pending commitments");
        LeaveBalance saved = store.save(new LeaveBalance(id, old.employeeId(), old.leaveTypeId(),
                old.balanceYear(), old.openingDays(), old.accruedDays(), old.usedDays(), adjusted, old.updatedAt()));
        event("LEAVE_BALANCE_ADJUSTED", "LEAVE_BALANCE", id,
                Map.of("actorId", who, "deltaDays", delta, "reason", reason,
                        "before", old.adjustedDays(), "after", adjusted));
        return balance(saved);
    }

    @Transactional public RequestResponse createRequest(RequestInput input) {
        operational(); lockEmployee(input.employeeId());
        LeaveType type = activeType(input.leaveTypeId());
        BigDecimal duration = duration(input, type);
        UUID actor = actor();
        LeaveRequest saved = store.save(new LeaveRequest(UUID.randomUUID(), input.employeeId(),
                type.id(), input.startDate(), input.endDate(), input.startTime(), input.endTime(),
                duration, required(input.reason(), 0, "reason"), RequestStatus.DRAFT,
                actor, null, null, null, null, null, null));
        history(saved.id(), null, RequestStatus.DRAFT, null, actor);
        event("LEAVE_REQUEST_CREATED", "LEAVE_REQUEST", saved.id(), Map.of("employeeId", saved.employeeId()));
        return request(saved);
    }

    @Transactional(readOnly = true) public Page<RequestResponse> requests(UUID employee, UUID type,
            RequestStatus status, LocalDate from, LocalDate to, Pageable page) {
        operational(); if (from != null && to != null && to.isBefore(from)) throw new BusinessException("Invalid date filter");
        return store.requests(employee, type, status, from, to, page).map(this::request);
    }
    @Transactional(readOnly = true) public RequestResponse request(UUID id) { operational(); return request(requireRequest(id)); }

    @Transactional public RequestResponse updateRequest(UUID id, RequestInput input) {
        operational(); LeaveRequest observed = requireRequest(id); lockEmployee(observed.employeeId());
        LeaveRequest old = lockedRequest(id);
        if (old.status() != RequestStatus.DRAFT) throw new BusinessException("Only drafts can be edited");
        if (!old.employeeId().equals(input.employeeId())) throw new BusinessException("Employee cannot change");
        LeaveType type = activeType(input.leaveTypeId()); BigDecimal duration = duration(input, type);
        LeaveRequest saved = store.save(new LeaveRequest(id, old.employeeId(), type.id(), input.startDate(),
                input.endDate(), input.startTime(), input.endTime(), duration,
                required(input.reason(), 0, "reason"), old.status(), old.requestedBy(), old.submittedAt(),
                old.reviewedBy(), old.reviewedAt(), old.reviewComment(), old.createdAt(), old.updatedAt()));
        event("LEAVE_REQUEST_UPDATED", "LEAVE_REQUEST", id, Map.of()); return request(saved);
    }

    @Transactional public RequestResponse submit(UUID id) {
        operational(); LeaveRequest observed = requireRequest(id); lockEmployee(observed.employeeId());
        LeaveRequest old = lockedRequest(id);
        if (old.status() != RequestStatus.DRAFT) throw new BusinessException("Only drafts can be submitted");
        LeaveType type = activeType(old.leaveTypeId()); validateRequest(old, type);
        if (type.deductsBalance()) {
            LeaveBalance balance = lockedBalance(old);
            BigDecimal available = balance.available().subtract(store.pendingDays(old.employeeId(), type.id(), old.startDate().getYear()));
            if (available.compareTo(old.requestedDays()) < 0) throw new BusinessException("Insufficient uncommitted leave balance");
        }
        Instant now = Instant.now(); UUID who = actor();
        LeaveRequest pending = withStatus(old, RequestStatus.PENDING, now, null, null, null);
        pending = store.save(pending);
        history(id, RequestStatus.DRAFT, RequestStatus.PENDING, null, who);
        event("LEAVE_REQUEST_SUBMITTED", "LEAVE_REQUEST", id, Map.of());
        if (type.requiresApproval()) return request(pending);
        return request(approveInternal(pending, type, null, true));
    }

    @Transactional public RequestResponse approve(UUID id, String comment) {
        operational(); LeaveRequest observed = requireRequest(id); lockEmployee(observed.employeeId());
        LeaveRequest old = lockedRequest(id);
        if (old.status() != RequestStatus.PENDING) throw new BusinessException("Only pending requests can be approved");
        LeaveType type = requireType(old.leaveTypeId());
        if (!type.requiresApproval()) throw new BusinessException("This leave type uses automatic approval");
        UUID who = actor();
        if (old.requestedBy() == null || old.requestedBy().equals(who))
            throw new BusinessException("Requester cannot approve their own leave request");
        return request(approveInternal(old, type, comment, false));
    }

    @Transactional public RequestResponse reject(UUID id, String comment) {
        operational(); LeaveRequest observed = requireRequest(id); lockEmployee(observed.employeeId());
        LeaveRequest old = lockedRequest(id);
        if (old.status() != RequestStatus.PENDING) throw new BusinessException("Only pending requests can be rejected");
        String reason = required(comment, 0, "reviewComment"); UUID who = actor(); Instant now = Instant.now();
        LeaveRequest saved = store.save(withStatus(old, RequestStatus.REJECTED, old.submittedAt(), who, now, reason));
        history(id, RequestStatus.PENDING, RequestStatus.REJECTED, reason, who);
        event("LEAVE_REQUEST_REJECTED", "LEAVE_REQUEST", id, Map.of()); return request(saved);
    }

    @Transactional public RequestResponse cancel(UUID id, String comment) {
        operational(); LeaveRequest observed = requireRequest(id); lockEmployee(observed.employeeId());
        LeaveRequest old = lockedRequest(id);
        if (old.status() != RequestStatus.DRAFT && old.status() != RequestStatus.PENDING
                && old.status() != RequestStatus.APPROVED)
            throw new BusinessException("Request cannot be cancelled from its current state");
        if (old.status() == RequestStatus.APPROVED) {
            if (!hasNotStarted(old))
                throw new BusinessException("Started leave requires administrative review; automatic refund is unavailable");
            LeaveType type = requireType(old.leaveTypeId());
            if (type.deductsBalance()) {
                LeaveBalance balance = lockedBalance(old);
                if (balance.usedDays().compareTo(old.requestedDays()) < 0)
                    throw new BusinessException("Recorded balance cannot be refunded safely");
                store.save(new LeaveBalance(balance.id(), balance.employeeId(), balance.leaveTypeId(),
                        balance.balanceYear(), balance.openingDays(), balance.accruedDays(),
                        balance.usedDays().subtract(old.requestedDays()), balance.adjustedDays(), balance.updatedAt()));
                event("LEAVE_BALANCE_RESTORED", "LEAVE_BALANCE", balance.id(),
                        Map.of("requestId", id, "days", old.requestedDays()));
            }
        }
        UUID who = actor();
        LeaveRequest saved = store.save(withStatus(old, RequestStatus.CANCELLED, old.submittedAt(),
                old.reviewedBy(), old.reviewedAt(), old.reviewComment()));
        history(id, old.status(), RequestStatus.CANCELLED, comment, who);
        event("LEAVE_REQUEST_CANCELLED", "LEAVE_REQUEST", id, Map.of("previousStatus", old.status()));
        if (old.status() == RequestStatus.APPROVED)
            attendanceSync.synchronize(saved.employeeId(), saved.startDate(), saved.endDate());
        return request(saved);
    }

    @Transactional(readOnly = true) public Page<HistoryResponse> history(UUID id, Instant from,
            Instant to, RequestStatus status, Pageable page) {
        operational(); requireRequest(id);
        if (from != null && to != null && to.isBefore(from)) throw new BusinessException("Invalid history date filter");
        return store.history(id, from, to, status, page).map(v -> new HistoryResponse(v.id(), v.leaveRequestId(),
                v.previousStatus(), v.newStatus(), v.comment(), v.performedBy(), v.createdAt()));
    }

    private LeaveRequest approveInternal(LeaveRequest old, LeaveType type, String comment, boolean automatic) {
        validateRequest(old, type);
        if (type.deductsBalance()) {
            LeaveBalance balance = lockedBalance(old);
            if (balance.available().compareTo(old.requestedDays()) < 0)
                throw new BusinessException("Insufficient leave balance");
            if (balance.usedDays().add(old.requestedDays()).compareTo(MAX) > 0)
                throw new BusinessException("Consumed days exceed balance precision");
            store.save(new LeaveBalance(balance.id(), balance.employeeId(), balance.leaveTypeId(),
                    balance.balanceYear(), balance.openingDays(), balance.accruedDays(),
                    balance.usedDays().add(old.requestedDays()), balance.adjustedDays(), balance.updatedAt()));
            event("LEAVE_BALANCE_CONSUMED", "LEAVE_BALANCE", balance.id(),
                    Map.of("requestId", old.id(), "days", old.requestedDays()));
        }
        UUID reviewer = automatic ? null : actor(); Instant now = Instant.now();
        LeaveRequest saved = store.save(withStatus(old, RequestStatus.APPROVED, old.submittedAt(),
                reviewer, now, comment));
        history(old.id(), RequestStatus.PENDING, RequestStatus.APPROVED, comment, reviewer);
        event(automatic ? "LEAVE_REQUEST_AUTO_APPROVED" : "LEAVE_REQUEST_APPROVED", "LEAVE_REQUEST",
                old.id(), Map.of("automatic", automatic));
        attendanceSync.synchronize(saved.employeeId(), saved.startDate(), saved.endDate());
        return saved;
    }

    private void validateRequest(LeaveRequest request, LeaveType type) {
        if (type.deductsBalance() && request.startDate().getYear() != request.endDate().getYear())
            throw new BusinessException("Balance-deducting leave cannot cross calendar years");
        if (type.requiresAttachment()) attachments.requireStoredAttachment(request.id());
        BigDecimal current = calculator.calculate(request.employeeId(), request.startDate(), request.endDate(),
                request.startTime(), request.endTime());
        if (current.compareTo(request.requestedDays()) != 0)
            throw new BusinessException("Scheduled duration changed; update the draft before submission");
        LocalDate searchFrom = request.startDate().equals(LocalDate.MIN)
                ? request.startDate() : request.startDate().minusDays(1);
        LocalDate searchTo = request.endDate().equals(LocalDate.MAX)
                ? request.endDate() : request.endDate().plusDays(1);
        for (LeaveRequest other : store.conflictingCandidates(request.employeeId(), searchFrom,
                searchTo, request.id())) {
            if (overlaps(request, other)) throw new BusinessException("Overlapping leave request exists");
        }
    }

    private boolean overlaps(LeaveRequest a, LeaveRequest b) {
        LocalDate first = a.startDate().isAfter(b.startDate()) ? a.startDate() : b.startDate();
        LocalDate last = a.endDate().isBefore(b.endDate()) ? a.endDate() : b.endDate();
        if (first.isAfter(last)) {
            if (a.startTime() == null && b.startTime() != null)
                return calculator.nightContinuationOverlaps(a.employeeId(), a.endDate(),
                        b.startDate(), b.startTime(), b.endTime());
            if (b.startTime() == null && a.startTime() != null)
                return calculator.nightContinuationOverlaps(a.employeeId(), b.endDate(),
                        a.startDate(), a.startTime(), a.endTime());
            return false;
        }
        if (a.startTime() == null || b.startTime() == null) return true;
        LocalTime overlapStart = a.startTime().isAfter(b.startTime()) ? a.startTime() : b.startTime();
        LocalTime overlapEnd = a.endTime().isBefore(b.endTime()) ? a.endTime() : b.endTime();
        return overlapEnd.isAfter(overlapStart)
                && calculator.hasWorkingMinutes(a.employeeId(), first, overlapStart, overlapEnd);
    }

    private BigDecimal duration(RequestInput input, LeaveType type) {
        if (input.startDate() == null || input.endDate() == null) throw new BusinessException("Dates are required");
        if (type.deductsBalance() && input.startDate().getYear() != input.endDate().getYear())
            throw new BusinessException("Balance-deducting leave cannot cross calendar years");
        return calculator.calculate(input.employeeId(), input.startDate(), input.endDate(),
                input.startTime(), input.endTime());
    }
    private LeaveBalance lockedBalance(LeaveRequest request) {
        return store.lockedBalance(request.employeeId(), request.leaveTypeId(), request.startDate().getYear())
                .orElseThrow(() -> new BusinessException("Annual leave balance must be initialized"));
    }
    private void lockEmployee(UUID id) {
        if (id == null || employees.findByIdForUpdate(id).isEmpty())
            throw new ResourceNotFoundException("Employee not found");
    }
    private LeaveType requireType(UUID id) {
        return store.type(id).orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));
    }
    private LeaveType activeType(UUID id) {
        LeaveType type = lockedType(id);
        if (type.status() != TypeStatus.ACTIVE) throw new BusinessException("Leave type is inactive");
        return type;
    }
    private LeaveBalance requireBalance(UUID id) {
        return store.balance(id).orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));
    }
    private LeaveType lockedType(UUID id) {
        return store.lockedType(id).orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));
    }
    private boolean hasNotStarted(LeaveRequest request) {
        String configured = settings.findByKey(TenantSettingKeys.COMPANY_TIMEZONE)
                .map(v -> v.settingValue()).map(String::trim).orElse("");
        if (!ZoneId.getAvailableZoneIds().contains(configured))
            throw new BusinessException("Configure company.timezone with a valid IANA time zone");
        ZoneId zone = ZoneId.of(configured);
        LocalDateTime start = request.startDate().atTime(
                request.startTime() == null ? LocalTime.MIN : request.startTime());
        if (zone.getRules().getValidOffsets(start).size() != 1)
            throw new BusinessException("Leave start time is ambiguous in tenant time zone");
        return ZonedDateTime.now(zone).toLocalDateTime().isBefore(start);
    }
    private LeaveRequest requireRequest(UUID id) {
        return store.request(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
    }
    private LeaveRequest lockedRequest(UUID id) {
        return store.lockedRequest(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
    }
    private LeaveRequest withStatus(LeaveRequest r, RequestStatus status, Instant submitted,
            UUID reviewer, Instant reviewed, String reviewComment) {
        return new LeaveRequest(r.id(), r.employeeId(), r.leaveTypeId(), r.startDate(), r.endDate(),
                r.startTime(), r.endTime(), r.requestedDays(), r.reason(), status, r.requestedBy(),
                submitted, reviewer, reviewed, reviewComment, r.createdAt(), r.updatedAt());
    }
    private void history(UUID id, RequestStatus before, RequestStatus after, String comment, UUID actor) {
        store.save(new LeaveRequestHistory(UUID.randomUUID(), id, before, after, comment, actor, null));
    }
    private void operational() { plans.assertCanOperate(); }
    private UUID actor() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }
    private void event(String action, String resource, UUID id, Object details) {
        audit.recordTenantEvent(action, resource, id.toString(), details);
    }
    private String required(String value, int max, String field) {
        if (value == null || value.isBlank()) throw new BusinessException(field + " is required");
        String result = value.trim();
        if (max > 0 && result.length() > max) throw new BusinessException(field + " exceeds " + max + " characters");
        return result;
    }
    private boolean required(Boolean value) {
        if (value == null) throw new BusinessException("Leave type properties are required");
        return value;
    }
    private void validateYear(Integer year) {
        if (year == null || year < 1900 || year > 9999) throw new BusinessException("Invalid balance year");
    }
    private BigDecimal days(BigDecimal value, boolean signed) {
        if (value == null || value.scale() > 2 || value.abs().compareTo(MAX) > 0 || (!signed && value.signum() < 0))
            throw new BusinessException("Invalid day amount; use up to two decimal places");
        return value.setScale(2);
    }
    private TypeResponse type(LeaveType v) {
        return new TypeResponse(v.id(), v.code(), v.name(), v.description(), v.paid(), v.requiresApproval(),
                v.requiresAttachment(), v.deductsBalance(), v.status(), v.createdAt(), v.updatedAt());
    }
    private BalanceResponse balance(LeaveBalance v) {
        return balance(v, store.pendingDays(v.employeeId(), v.leaveTypeId(), v.balanceYear()));
    }
    private BalanceResponse balance(LeaveBalance v, BigDecimal pending) {
        return new BalanceResponse(v.id(), v.employeeId(), v.leaveTypeId(), v.balanceYear(), v.openingDays(),
                v.accruedDays(), v.usedDays(), v.adjustedDays(), pending, v.available().subtract(pending), v.updatedAt());
    }
    private RequestResponse request(LeaveRequest v) {
        return new RequestResponse(v.id(), v.employeeId(), v.leaveTypeId(), v.startDate(), v.endDate(),
                v.startTime(), v.endTime(), v.requestedDays(), v.reason(), v.status(), v.requestedBy(),
                v.submittedAt(), v.reviewedBy(), v.reviewedAt(), v.reviewComment(), v.createdAt(), v.updatedAt());
    }
}
