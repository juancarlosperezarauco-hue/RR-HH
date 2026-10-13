package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmployeeDocumentJpaRepository
    extends JpaRepository<EmployeeDocumentEntity, UUID>,
        JpaSpecificationExecutor<EmployeeDocumentEntity> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from EmployeeDocumentEntity d where d.id=:id")
  Optional<EmployeeDocumentEntity> findByIdForUpdate(@Param("id") UUID id);
}
