package com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.ContractHistory;
import java.util.List;
import java.util.UUID;

public interface ContractHistoryRepository {
    ContractHistory save(ContractHistory history);
    List<ContractHistory> findByContractId(UUID contractId);
}
