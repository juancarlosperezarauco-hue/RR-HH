package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmployeeDocumentVersionJpaRepository
    extends JpaRepository<EmployeeDocumentVersionEntity, UUID> {
  Page<EmployeeDocumentVersionEntity> findByDocumentId(
      UUID documentId, org.springframework.data.domain.Pageable pageable);

  @Query(
      "select coalesce(max(v.versionNumber),0)+1 from EmployeeDocumentVersionEntity v where"
          + " v.documentId=:documentId")
  int nextVersionNumber(@Param("documentId") UUID documentId);
}
