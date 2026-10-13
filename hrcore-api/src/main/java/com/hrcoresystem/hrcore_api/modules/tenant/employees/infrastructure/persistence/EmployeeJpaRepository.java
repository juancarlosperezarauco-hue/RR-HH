package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID>, JpaSpecificationExecutor<EmployeeEntity> {
    boolean existsByEmployeeNumber(String number);
    boolean existsByEmployeeNumberAndIdNot(String number, UUID id);
    boolean existsByDocumentTypeAndDocumentNumber(String type, String number);
    boolean existsByDocumentTypeAndDocumentNumberAndIdNot(String type, String number, UUID id);
    boolean existsByTenantUserId(UUID tenantUserId);
    boolean existsByTenantUserIdAndIdNot(UUID tenantUserId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmployeeEntity e where e.id = :id")
    Optional<EmployeeEntity> findByIdForUpdate(@Param("id") UUID id);
}
