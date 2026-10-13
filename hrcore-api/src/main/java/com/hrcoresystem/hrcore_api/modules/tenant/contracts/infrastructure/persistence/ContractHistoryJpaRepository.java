package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ContractHistoryJpaRepository extends JpaRepository<ContractHistoryEntity, UUID> {
    List<ContractHistoryEntity> findByContractIdOrderByCreatedAtDesc(UUID contractId);
}
