package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.EmploymentContractRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.UUID;

@Component
public class ContractPolicy {
    private final EmploymentContractRepository contracts;
    private final EmployeeRepository employees;
    private final EmployeeAssignmentRepository assignments;
    private final TenantPlanEnforcementService plans;
    private final SecurityContextFacade security;

    public ContractPolicy(EmploymentContractRepository contracts, EmployeeRepository employees,
                          EmployeeAssignmentRepository assignments, TenantPlanEnforcementService plans,
                          SecurityContextFacade security) {
        this.contracts = contracts; this.employees = employees; this.assignments = assignments;
        this.plans = plans; this.security = security;
    }
    public void assertOperational() { plans.assertCanOperate(); }
    public UUID actorId() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }
    public EmploymentContract require(UUID id) {
        return contracts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
    }
    public EmploymentContract requireForUpdate(UUID id) {
        return contracts.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
    }
    public EmploymentContract normalized(UUID id, ContractRequest r, EmploymentContract before, UUID actor) {
        if (before != null && before.status() != ContractStatus.DRAFT && before.status() != ContractStatus.PENDING_SIGNATURE)
            throw new BusinessException("Only draft or pending-signature contracts can be edited");
        validateCommon(id, r.employeeId(), r.employeeAssignmentId(), r.contractNumber(), r.contractType(),
                r.startDate(), r.endDate(), r.probationEndDate());
        return new EmploymentContract(id, r.employeeId(), r.employeeAssignmentId(), r.contractNumber().trim(),
                r.contractType(), before == null ? ContractStatus.DRAFT : before.status(), r.contractForm(),
                r.workSchedule(), r.workModality(), r.startDate(), normalizedEnd(r.contractType(), r.endDate()),
                before == null ? null : before.signedAt(), r.baseSalary(), r.currency().trim().toUpperCase(),
                r.paymentFrequency(), r.weeklyHours(), r.probationEndDate(),
                before == null ? null : before.terminationDate(), before == null ? null : before.terminationReason(),
                trim(r.terms()), before == null ? null : before.renewedFromContractId(),
                before == null ? actor : before.createdBy(), actor,
                before == null ? null : before.createdAt(), before == null ? null : before.updatedAt());
    }
    public EmploymentContract renewal(EmploymentContract source, ContractRenewalRequest r, UUID actor) {
        if (source.contractType() == ContractType.INDEFINITE)
            throw new BusinessException("Indefinite contracts are changed through an amendment, not renewal");
        if (source.status() != ContractStatus.ACTIVE && source.status() != ContractStatus.EXPIRED)
            throw new BusinessException("Only active or expired contracts can be renewed");
        if (contracts.existsRenewal(source.id())) throw new BusinessException("This contract already has a renewal");
        if (source.endDate() == null || !r.startDate().isAfter(source.endDate()))
            throw new BusinessException("Renewal startDate must be after the current contract endDate");
        validateCommon(null, source.employeeId(), r.employeeAssignmentId(), r.contractNumber(), r.contractType(),
                r.startDate(), r.endDate(), r.probationEndDate());
        return new EmploymentContract(null, source.employeeId(), r.employeeAssignmentId(), r.contractNumber().trim(),
                r.contractType(), ContractStatus.DRAFT, r.contractForm(), r.workSchedule(), r.workModality(),
                r.startDate(), normalizedEnd(r.contractType(), r.endDate()), null, r.baseSalary(),
                r.currency().trim().toUpperCase(), r.paymentFrequency(), r.weeklyHours(), r.probationEndDate(),
                null, null, trim(r.terms()), source.id(), actor, actor, null, null);
    }
    public EmploymentContract changeStatus(EmploymentContract c, ContractStatus target,
                                            LocalDate effectiveDate, String reason, UUID actor) {
        if (target == null || target == c.status()) throw new BusinessException("A different target status is required");
        boolean allowed = switch (c.status()) {
            case DRAFT -> target == ContractStatus.PENDING_SIGNATURE || target == ContractStatus.CANCELLED;
            case PENDING_SIGNATURE -> target == ContractStatus.ACTIVE || target == ContractStatus.CANCELLED;
            case ACTIVE -> target == ContractStatus.TERMINATED || target == ContractStatus.EXPIRED;
            default -> false;
        };
        if (!allowed) throw new BusinessException("Invalid contract status transition: " + c.status() + " -> " + target);
        Instant signedAt = c.signedAt(); LocalDate terminationDate = c.terminationDate(); String terminationReason = c.terminationReason();
        if (target == ContractStatus.ACTIVE) {
            validateAssignment(c.employeeId(), c.employeeAssignmentId(), c.startDate());
            if (contracts.existsOverlappingActive(c.employeeId(), c.id(), c.startDate(), c.endDate()))
                throw new BusinessException("Employee already has an active contract in this period");
            signedAt = Instant.now();
        }
        if (target == ContractStatus.TERMINATED) {
            terminationDate = effectiveDate == null ? LocalDate.now() : effectiveDate;
            if (terminationDate.isBefore(c.startDate())) throw new BusinessException("terminationDate cannot precede startDate");
            if (reason == null || reason.isBlank()) throw new BusinessException("Termination reason is required");
            terminationReason = reason.trim();
        }
        return new EmploymentContract(c.id(), c.employeeId(), c.employeeAssignmentId(), c.contractNumber(),
                c.contractType(), target, c.contractForm(), c.workSchedule(), c.workModality(), c.startDate(),
                c.endDate(), signedAt, c.baseSalary(), c.currency(), c.paymentFrequency(), c.weeklyHours(),
                c.probationEndDate(), terminationDate, terminationReason, c.terms(), c.renewedFromContractId(),
                c.createdBy(), actor, c.createdAt(), c.updatedAt());
    }
    private void validateCommon(UUID id, UUID employeeId, UUID assignmentId, String number, ContractType type,
                                LocalDate start, LocalDate end, LocalDate probationEnd) {
        assertOperational();
        if (number == null || number.isBlank()) throw new BusinessException("contractNumber is required");
        if (contracts.existsContractNumberOtherThan(number.trim(), id))
            throw new BusinessException("Contract number already exists");
        Employee employee = employees.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee does not exist"));
        if (employee.employmentStatus() == EmploymentStatus.TERMINATED || employee.employmentStatus() == EmploymentStatus.INACTIVE)
            throw new BusinessException("Employee must be active to receive a contract");
        if (start == null) throw new BusinessException("startDate is required");
        if (start.isBefore(employee.hireDate())) throw new BusinessException("Contract cannot start before employee hireDate");
        validateAssignment(employeeId, assignmentId, start);
        if (type != ContractType.INDEFINITE && end == null)
            throw new BusinessException("endDate is required for this contract type");
        if (end != null && end.isBefore(start)) throw new BusinessException("endDate cannot precede startDate");
        if (probationEnd != null && (probationEnd.isBefore(start) || (end != null && probationEnd.isAfter(end))))
            throw new BusinessException("probationEndDate must be within the contract period");
    }
    private void validateAssignment(UUID employeeId, UUID assignmentId, LocalDate date) {
        EmployeeAssignment assignment = assignments.findByIdAndEmployeeId(assignmentId, employeeId)
                .orElseThrow(() -> new BusinessException("Employee assignment does not exist"));
        if (assignment.startDate().isAfter(date) || (assignment.endDate() != null && assignment.endDate().isBefore(date)))
            throw new BusinessException("Employee assignment is not valid on contract startDate");
    }
    private LocalDate normalizedEnd(ContractType type, LocalDate end) { return type == ContractType.INDEFINITE ? null : end; }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
