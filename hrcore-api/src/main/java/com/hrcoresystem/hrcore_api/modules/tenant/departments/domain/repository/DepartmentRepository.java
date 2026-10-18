package com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.Department;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface DepartmentRepository {
    Department save(Department department);
    Optional<Department> findById(UUID id);
    boolean existsByCodeOtherThan(String code, UUID id);
    Page<Department> findAll(DepartmentFilter filter, Pageable pageable);
    boolean existsActiveEmployee(UUID employeeId);
    boolean hasActiveChildren(UUID departmentId);
    void lockHierarchy();
    List<Department> findAllForTree(boolean includeInactive);
    boolean hasCurrentOrFutureAssignments(UUID departmentId, java.time.LocalDate asOfDate);
}
