package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface EmployeeDocumentJpaRepository
    extends JpaRepository<EmployeeDocumentEntity, UUID>,
        JpaSpecificationExecutor<EmployeeDocumentEntity> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from EmployeeDocumentEntity d where d.id=:id")
  Optional<EmployeeDocumentEntity> findByIdForUpdate(@Param("id") UUID id);
  @Query("select d from EmployeeDocumentEntity d where d.expiresAt is not null and d.expiresAt <= :until and d.status = com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.FileStatus.ACTIVE order by d.expiresAt asc")
  List<EmployeeDocumentEntity> findExpiring(@Param("until") java.time.LocalDate until, Pageable pageable);
}
