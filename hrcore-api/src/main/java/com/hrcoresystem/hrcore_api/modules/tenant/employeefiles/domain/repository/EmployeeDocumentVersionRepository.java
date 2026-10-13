package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.EmployeeDocumentVersion;
import java.util.*;
import org.springframework.data.domain.*;

public interface EmployeeDocumentVersionRepository {
  EmployeeDocumentVersion save(EmployeeDocumentVersion version);

  Optional<EmployeeDocumentVersion> findById(UUID id);

  Page<EmployeeDocumentVersion> findAll(UUID documentId, Pageable pageable);

  int nextVersionNumber(UUID documentId);
}
