package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface EmploymentContractJpaRepository extends JpaRepository<EmploymentContractEntity, UUID>,
        JpaSpecificationExecutor<EmploymentContractEntity> {
    boolean existsByContractNumber(String contractNumber);
    boolean existsByContractNumberAndIdNot(String contractNumber, UUID id);
    boolean existsByRenewedFromContractId(UUID renewedFromContractId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmploymentContractEntity c where c.id = :id")
    Optional<EmploymentContractEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        select (count(c) > 0) from EmploymentContractEntity c
        where c.employeeId = :employeeId and c.status = com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.ContractStatus.ACTIVE
          and (:exceptId is null or c.id <> :exceptId)
          and (c.endDate is null or c.endDate >= :startDate)
          and (:endDate is null or c.startDate <= :endDate)
        """)
    boolean existsOverlappingActive(@Param("employeeId") UUID employeeId,
                                    @Param("exceptId") UUID exceptId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}
