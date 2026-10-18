package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface EmploymentContractJpaRepository extends JpaRepository<EmploymentContractEntity, UUID>,
        JpaSpecificationExecutor<EmploymentContractEntity> {
    boolean existsByContractNumber(String contractNumber);
    boolean existsByContractNumberAndIdNot(String contractNumber, UUID id);
    boolean existsByRenewedFromContractId(UUID renewedFromContractId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmploymentContractEntity c where c.status = com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.ContractStatus.ACTIVE and c.endDate is not null and c.endDate < :today order by c.id")
    List<EmploymentContractEntity> findExpiredBatch(@Param("today") LocalDate today, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmploymentContractEntity c where c.employeeId=:employeeId order by c.id")
    List<EmploymentContractEntity> findAllByEmployeeIdForUpdate(@Param("employeeId") UUID employeeId);

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
