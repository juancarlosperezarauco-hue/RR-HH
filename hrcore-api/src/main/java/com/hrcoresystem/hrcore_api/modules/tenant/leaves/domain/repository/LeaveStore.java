package com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeaveStore {
    LeaveType save(LeaveType value);
    Optional<LeaveType> type(UUID id);
    Optional<LeaveType> lockedType(UUID id);
    boolean typeCodeExists(String code, UUID exceptId);
    boolean typeHasRelevantRequests(UUID id);
    Page<LeaveType> types(String code, String name, TypeStatus status, Pageable page);

    LeaveBalance save(LeaveBalance value);
    Optional<LeaveBalance> balance(UUID id);
    Optional<LeaveBalance> lockedBalance(UUID employeeId, UUID typeId, int year);
    boolean balanceExists(UUID employeeId, UUID typeId, int year);
    Page<LeaveBalance> balances(UUID employeeId, UUID typeId, Integer year, Pageable page);
    BigDecimal pendingDays(UUID employeeId, UUID typeId, int year);
    Map<UUID, BigDecimal> pendingDaysForBalances(List<LeaveBalance> balances);

    LeaveRequest save(LeaveRequest value);
    Optional<LeaveRequest> request(UUID id);
    Optional<LeaveRequest> lockedRequest(UUID id);
    Page<LeaveRequest> requests(UUID employeeId, UUID typeId, RequestStatus status,
            LocalDate from, LocalDate to, Pageable page);
    List<LeaveRequest> conflictingCandidates(UUID employeeId, LocalDate from, LocalDate to, UUID exceptId);
    List<LeaveRequest> approvedOn(UUID employeeId, LocalDate date);

    LeaveRequestHistory save(LeaveRequestHistory value);
    Page<LeaveRequestHistory> history(UUID requestId, Instant from, Instant to,
            RequestStatus status, Pageable page);

    LeaveAttachment save(LeaveAttachment value);
    Optional<LeaveAttachment> attachment(UUID id);
    Page<LeaveAttachment> attachments(UUID requestId, Pageable page);
    boolean hasAttachment(UUID requestId);
}
