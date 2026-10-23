package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import java.util.*;
import java.time.LocalDate;
import org.springframework.data.domain.*;

public interface EmployeeDocumentRepository {
  EmployeeDocument save(EmployeeDocument document);

  Optional<EmployeeDocument> findById(UUID id);

  Optional<EmployeeDocument> findByIdForUpdate(UUID id);

  Page<EmployeeDocument> findAll(UUID fileId, EmployeeDocumentFilter filter, Pageable pageable);

  List<EmployeeDocument> findExpiring(LocalDate until, Pageable pageable);
}
