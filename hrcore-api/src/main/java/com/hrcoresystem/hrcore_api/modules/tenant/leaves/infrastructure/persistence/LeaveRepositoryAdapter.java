package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class LeaveRepositoryAdapter implements LeaveStore {
    private final LeaveTypeJpaRepository types;
    private final LeaveBalanceJpaRepository balances;
    private final LeaveRequestJpaRepository requests;
    private final LeaveHistoryJpaRepository histories;
    private final LeaveAttachmentJpaRepository attachments;

    public LeaveRepositoryAdapter(LeaveTypeJpaRepository types, LeaveBalanceJpaRepository balances,
            LeaveRequestJpaRepository requests, LeaveHistoryJpaRepository histories,
            LeaveAttachmentJpaRepository attachments) {
        this.types = types; this.balances = balances; this.requests = requests;
        this.histories = histories; this.attachments = attachments;
    }

    @Override public LeaveType save(LeaveType v) {
        var e = types.findById(v.id()).orElseGet(LeaveTypeEntity::new);
        e.setId(v.id()); e.setCode(v.code()); e.setName(v.name()); e.setDescription(v.description());
        e.setPaid(v.paid()); e.setRequiresApproval(v.requiresApproval());
        e.setRequiresAttachment(v.requiresAttachment()); e.setDeductsBalance(v.deductsBalance());
        e.setStatus(v.status()); return type(types.saveAndFlush(e));
    }
    @Override public Optional<LeaveType> type(UUID id) { return types.findById(id).map(this::type); }
    @Override public Optional<LeaveType> lockedType(UUID id) { return types.lock(id).map(this::type); }
    @Override public boolean typeCodeExists(String code, UUID exceptId) {
        return exceptId == null ? types.existsByCode(code) : types.existsByCodeAndIdNot(code, exceptId);
    }
    @Override public boolean typeHasRelevantRequests(UUID id) {
        return requests.existsByLeaveTypeIdAndStatusIn(id, List.of(RequestStatus.PENDING,
                RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED));
    }
    @Override public Page<LeaveType> types(String code, String name, TypeStatus status, Pageable page) {
        Specification<LeaveTypeEntity> spec = (root, query, cb) -> cb.and(
                code == null ? cb.conjunction() : cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase(Locale.ROOT) + "%"),
                name == null ? cb.conjunction() : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase(Locale.ROOT) + "%"),
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status));
        return types.findAll(spec, page).map(this::type);
    }

    @Override public LeaveBalance save(LeaveBalance v) {
        var e = balances.findById(v.id()).orElseGet(LeaveBalanceEntity::new);
        e.setId(v.id()); e.setEmployeeId(v.employeeId()); e.setLeaveTypeId(v.leaveTypeId());
        e.setBalanceYear(v.balanceYear()); e.setOpeningDays(v.openingDays());
        e.setAccruedDays(v.accruedDays()); e.setUsedDays(v.usedDays());
        e.setAdjustedDays(v.adjustedDays()); return balance(balances.saveAndFlush(e));
    }
    @Override public Optional<LeaveBalance> balance(UUID id) { return balances.findById(id).map(this::balance); }
    @Override public Optional<LeaveBalance> lockedBalance(UUID employeeId, UUID typeId, int year) {
        return balances.lock(employeeId, typeId, year).map(this::balance);
    }
    @Override public boolean balanceExists(UUID employeeId, UUID typeId, int year) {
        return balances.existsByEmployeeIdAndLeaveTypeIdAndBalanceYear(employeeId, typeId, year);
    }
    @Override public Page<LeaveBalance> balances(UUID employeeId, UUID typeId, Integer year, Pageable page) {
        Specification<LeaveBalanceEntity> spec = (root, query, cb) -> cb.and(
                employeeId == null ? cb.conjunction() : cb.equal(root.get("employeeId"), employeeId),
                typeId == null ? cb.conjunction() : cb.equal(root.get("leaveTypeId"), typeId),
                year == null ? cb.conjunction() : cb.equal(root.get("balanceYear"), year));
        return balances.findAll(spec, page).map(this::balance);
    }
    @Override public BigDecimal pendingDays(UUID employeeId, UUID typeId, int year) {
        return requests.pending(employeeId, typeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
                RequestStatus.PENDING);
    }
    @Override public Map<UUID, BigDecimal> pendingDaysForBalances(List<LeaveBalance> values) {
        if (values.isEmpty()) return Map.of();
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (Object[] row : balances.pendingForBalances(values.stream().map(LeaveBalance::id).toList()))
            result.put((UUID) row[0], (BigDecimal) row[1]);
        return result;
    }

    @Override public LeaveRequest save(LeaveRequest v) {
        var e = requests.findById(v.id()).orElseGet(LeaveRequestEntity::new);
        e.setId(v.id()); e.setEmployeeId(v.employeeId()); e.setLeaveTypeId(v.leaveTypeId());
        e.setStartDate(v.startDate()); e.setEndDate(v.endDate());
        e.setStartTime(v.startTime()); e.setEndTime(v.endTime()); e.setRequestedDays(v.requestedDays());
        e.setReason(v.reason()); e.setStatus(v.status()); e.setRequestedBy(v.requestedBy());
        e.setSubmittedAt(v.submittedAt()); e.setReviewedBy(v.reviewedBy());
        e.setReviewedAt(v.reviewedAt()); e.setReviewComment(v.reviewComment());
        return request(requests.saveAndFlush(e));
    }
    @Override public Optional<LeaveRequest> request(UUID id) { return requests.findById(id).map(this::request); }
    @Override public Optional<LeaveRequest> lockedRequest(UUID id) { return requests.lock(id).map(this::request); }
    @Override public Page<LeaveRequest> requests(UUID employeeId, UUID typeId, RequestStatus status,
            LocalDate from, LocalDate to, Pageable page) {
        Specification<LeaveRequestEntity> spec = (root, query, cb) -> cb.and(
                employeeId == null ? cb.conjunction() : cb.equal(root.get("employeeId"), employeeId),
                typeId == null ? cb.conjunction() : cb.equal(root.get("leaveTypeId"), typeId),
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status),
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("endDate"), from),
                to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("startDate"), to));
        return requests.findAll(spec, page).map(this::request);
    }
    @Override public List<LeaveRequest> conflictingCandidates(UUID employeeId, LocalDate from,
            LocalDate to, UUID exceptId) {
        return requests.candidates(employeeId, from, to, exceptId,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED)).stream().map(this::request).toList();
    }
    @Override public List<LeaveRequest> approvedOn(UUID employeeId, LocalDate date) {
        return requests.approvedOn(employeeId, date, RequestStatus.APPROVED).stream().map(this::request).toList();
    }

    @Override public LeaveRequestHistory save(LeaveRequestHistory v) {
        var e = new LeaveRequestHistoryEntity(); e.setId(v.id()); e.setLeaveRequestId(v.leaveRequestId());
        e.setPreviousStatus(v.previousStatus()); e.setNewStatus(v.newStatus());
        e.setComment(v.comment()); e.setPerformedBy(v.performedBy());
        return history(histories.saveAndFlush(e));
    }
    @Override public Page<LeaveRequestHistory> history(UUID requestId, Instant from, Instant to,
            RequestStatus status, Pageable page) {
        Specification<LeaveRequestHistoryEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("leaveRequestId"), requestId),
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), from),
                to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), to),
                status == null ? cb.conjunction() : cb.equal(root.get("newStatus"), status));
        return histories.findAll(spec, page).map(this::history);
    }

    @Override public LeaveAttachment save(LeaveAttachment v) {
        var e = new LeaveAttachmentEntity(); e.setId(v.id()); e.setLeaveRequestId(v.leaveRequestId());
        e.setFilename(v.filename()); e.setStorageKey(v.storageKey()); e.setMimeType(v.mimeType());
        e.setFileSize(v.fileSize()); e.setUploadedBy(v.uploadedBy());
        return attachment(attachments.saveAndFlush(e));
    }
    @Override public Optional<LeaveAttachment> attachment(UUID id) {
        return attachments.findById(id).map(this::attachment);
    }
    @Override public Page<LeaveAttachment> attachments(UUID requestId, Pageable page) {
        return attachments.findByLeaveRequestId(requestId, page).map(this::attachment);
    }
    @Override public boolean hasAttachment(UUID requestId) { return attachments.existsByLeaveRequestId(requestId); }

    private LeaveType type(LeaveTypeEntity e) {
        return new LeaveType(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.isPaid(),
                e.isRequiresApproval(), e.isRequiresAttachment(), e.isDeductsBalance(),
                e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private LeaveBalance balance(LeaveBalanceEntity e) {
        return new LeaveBalance(e.getId(), e.getEmployeeId(), e.getLeaveTypeId(), e.getBalanceYear(),
                e.getOpeningDays(), e.getAccruedDays(), e.getUsedDays(), e.getAdjustedDays(), e.getUpdatedAt());
    }
    private LeaveRequest request(LeaveRequestEntity e) {
        return new LeaveRequest(e.getId(), e.getEmployeeId(), e.getLeaveTypeId(), e.getStartDate(),
                e.getEndDate(), e.getStartTime(), e.getEndTime(), e.getRequestedDays(), e.getReason(),
                e.getStatus(), e.getRequestedBy(), e.getSubmittedAt(), e.getReviewedBy(), e.getReviewedAt(),
                e.getReviewComment(), e.getCreatedAt(), e.getUpdatedAt());
    }
    private LeaveRequestHistory history(LeaveRequestHistoryEntity e) {
        return new LeaveRequestHistory(e.getId(), e.getLeaveRequestId(), e.getPreviousStatus(),
                e.getNewStatus(), e.getComment(), e.getPerformedBy(), e.getCreatedAt());
    }
    private LeaveAttachment attachment(LeaveAttachmentEntity e) {
        return new LeaveAttachment(e.getId(), e.getLeaveRequestId(), e.getFilename(), e.getStorageKey(),
                e.getMimeType(), e.getFileSize(), e.getUploadedBy(), e.getCreatedAt());
    }
}
