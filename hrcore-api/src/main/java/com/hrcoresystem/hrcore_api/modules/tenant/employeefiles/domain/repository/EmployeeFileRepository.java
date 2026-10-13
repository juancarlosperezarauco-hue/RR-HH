package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import java.util.*;
import org.springframework.data.domain.*;

public interface EmployeeFileRepository {
  EmployeeFile save(EmployeeFile file);

  Optional<EmployeeFile> findById(UUID id);

  Optional<EmployeeFile> findByIdForUpdate(UUID id);

  Optional<EmployeeFile> findByEmployeeId(UUID employeeId);

  boolean existsByFileNumberOtherThan(String fileNumber, UUID id);

  Page<EmployeeFile> findAll(EmployeeFileFilter filter, Pageable pageable);
}
