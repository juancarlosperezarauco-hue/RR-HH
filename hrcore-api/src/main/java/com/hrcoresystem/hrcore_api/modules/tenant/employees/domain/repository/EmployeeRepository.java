package com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID id);
    Optional<Employee> findByIdForUpdate(UUID id);
    Page<Employee> findAll(EmployeeFilter filter, Pageable pageable);
    boolean existsNumberOtherThan(String number, UUID id);
    boolean existsDocumentOtherThan(String type, String number, UUID id);
    boolean existsTenantUserOtherThan(UUID tenantUserId, UUID id);
}
