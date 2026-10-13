package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.ContractHistory;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.ContractHistoryRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class ContractHistoryRepositoryAdapter implements ContractHistoryRepository {
    private final ContractHistoryJpaRepository jpa;
    public ContractHistoryRepositoryAdapter(ContractHistoryJpaRepository jpa) { this.jpa = jpa; }
    @Override public ContractHistory save(ContractHistory h) {
        ContractHistoryEntity e = new ContractHistoryEntity();
        e.setContractId(h.contractId()); e.setAction(h.action()); e.setPreviousStatus(h.previousStatus());
        e.setNewStatus(h.newStatus()); e.setDetails(h.details()); e.setReason(h.reason());
        e.setPerformedBy(h.performedBy());
        return toDomain(jpa.saveAndFlush(e));
    }
    @Override public List<ContractHistory> findByContractId(UUID id) {
        return jpa.findByContractIdOrderByCreatedAtDesc(id).stream().map(this::toDomain).toList();
    }
    private ContractHistory toDomain(ContractHistoryEntity e) {
        return new ContractHistory(e.getId(), e.getContractId(), e.getAction(), e.getPreviousStatus(),
                e.getNewStatus(), e.getDetails(), e.getReason(), e.getPerformedBy(), e.getCreatedAt());
    }
}
