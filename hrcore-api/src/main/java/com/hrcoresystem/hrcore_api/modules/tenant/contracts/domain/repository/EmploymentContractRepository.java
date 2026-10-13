package com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface EmploymentContractRepository {
    EmploymentContract save(EmploymentContract contract);
    Optional<EmploymentContract> findById(UUID id);
    Optional<EmploymentContract> findByIdForUpdate(UUID id);
    Page<EmploymentContract> findAll(ContractFilter filter, Pageable pageable);
    boolean existsContractNumberOtherThan(String number, UUID id);
    boolean existsOverlappingActive(UUID employeeId, UUID exceptId, LocalDate startDate, LocalDate endDate);
    boolean existsRenewal(UUID sourceContractId);
}
