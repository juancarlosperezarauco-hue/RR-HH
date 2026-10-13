package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EmployeeFileJpaRepository
    extends JpaRepository<EmployeeFileEntity, UUID>, JpaSpecificationExecutor<EmployeeFileEntity> {
  Optional<EmployeeFileEntity> findByEmployeeId(UUID employeeId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from EmployeeFileEntity f where f.id = :id")
  Optional<EmployeeFileEntity> findByIdForUpdate(
      @org.springframework.data.repository.query.Param("id") UUID id);

  boolean existsByFileNumber(String number);

  boolean existsByFileNumberAndIdNot(String number, UUID id);
}
