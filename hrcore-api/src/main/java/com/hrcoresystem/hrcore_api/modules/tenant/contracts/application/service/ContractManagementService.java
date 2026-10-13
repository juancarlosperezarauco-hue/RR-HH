package com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service;

import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.mapper.ContractMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ContractManagementService {
    private final EmploymentContractRepository contracts;
    private final ContractHistoryRepository history;
    private final ContractPolicy policy;
    private final ContractMapper mapper;
    private final AuditTrailService audit;

    public ContractManagementService(EmploymentContractRepository contracts, ContractHistoryRepository history,
                                     ContractPolicy policy, ContractMapper mapper, AuditTrailService audit) {
        this.contracts = contracts; this.history = history; this.policy = policy;
        this.mapper = mapper; this.audit = audit;
    }

    @Transactional
    public ContractResponse create(ContractRequest request) {
        UUID actor = policy.actorId();
        EmploymentContract saved = contracts.save(policy.normalized(null, request, null, actor));
        record(saved.id(), ContractHistoryAction.CREATED, null, saved.status(),
                Map.of("contractNumber", saved.contractNumber(), "employeeId", saved.employeeId().toString()), null, actor);
        audit.recordTenantEvent("CONTRACT_CREATED", "EMPLOYMENT_CONTRACT", saved.id().toString(),
                Map.of("contractNumber", saved.contractNumber(), "employeeId", saved.employeeId()));
        return mapper.toResponse(saved);
    }

    public Page<ContractResponse> list(ContractFilter filter, Pageable pageable) {
        policy.assertOperational();
        return contracts.findAll(filter, pageable).map(mapper::toResponse);
    }

    public ContractResponse get(UUID id) {
        policy.assertOperational();
        return mapper.toResponse(policy.require(id));
    }

    @Transactional
    public ContractResponse update(UUID id, ContractRequest request) {
        UUID actor = policy.actorId();
        EmploymentContract before = policy.requireForUpdate(id);
        EmploymentContract saved = contracts.save(policy.normalized(id, request, before, actor));
        record(id, ContractHistoryAction.UPDATED, before.status(), saved.status(),
                Map.of("contractNumber", saved.contractNumber()), null, actor);
        audit.recordTenantEvent("CONTRACT_UPDATED", "EMPLOYMENT_CONTRACT", id.toString(),
                Map.of("contractNumber", saved.contractNumber()), before, saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public ContractResponse changeStatus(UUID id, ContractStatusRequest request) {
        policy.assertOperational();
        UUID actor = policy.actorId();
        EmploymentContract before = policy.requireForUpdate(id);
        EmploymentContract saved = contracts.save(policy.changeStatus(before, request.status(),
                request.effectiveDate(), request.reason(), actor));
        ContractHistoryAction action = switch (saved.status()) {
            case ACTIVE -> ContractHistoryAction.ACTIVATED;
            case EXPIRED -> ContractHistoryAction.EXPIRED;
            case TERMINATED -> ContractHistoryAction.TERMINATED;
            case CANCELLED -> ContractHistoryAction.CANCELLED;
            default -> ContractHistoryAction.UPDATED;
        };
        record(id, action, before.status(), saved.status(), Map.of(), request.reason(), actor);
        audit.recordTenantEvent("CONTRACT_STATUS_CHANGED", "EMPLOYMENT_CONTRACT", id.toString(),
                Map.of("previousStatus", before.status(), "newStatus", saved.status()), before, saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public ContractResponse renew(UUID id, ContractRenewalRequest request) {
        UUID actor = policy.actorId();
        EmploymentContract source = policy.requireForUpdate(id);
        EmploymentContract renewal = contracts.save(policy.renewal(source, request, actor));
        String reason = request.reason() == null || request.reason().isBlank() ? "Contract renewal" : request.reason().trim();
        record(source.id(), ContractHistoryAction.RENEWED, source.status(), source.status(),
                Map.of("renewalContractId", renewal.id().toString(), "renewalContractNumber", renewal.contractNumber()), reason, actor);
        record(renewal.id(), ContractHistoryAction.CREATED, null, renewal.status(),
                Map.of("renewedFromContractId", source.id().toString(), "previousContractNumber", source.contractNumber()), reason, actor);
        audit.recordTenantEvent("CONTRACT_RENEWED", "EMPLOYMENT_CONTRACT", source.id().toString(),
                Map.of("renewalContractId", renewal.id(), "renewalContractNumber", renewal.contractNumber()));
        return mapper.toResponse(renewal);
    }

    public List<ContractHistoryResponse> history(UUID id) {
        policy.assertOperational();
        policy.require(id);
        return history.findByContractId(id).stream().map(mapper::toResponse).toList();
    }

    private void record(UUID contractId, ContractHistoryAction action, ContractStatus previous,
                        ContractStatus current, Map<String, Object> details, String reason, UUID actor) {
        history.save(new ContractHistory(null, contractId, action, previous, current,
                details, reason, actor, null));
    }
}
