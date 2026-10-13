package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAssignmentJpaRepository extends JpaRepository<EmployeeAssignmentEntity, UUID> {
    Optional<EmployeeAssignmentEntity> findByIdAndEmployeeId(UUID id, UUID employeeId);
    Page<EmployeeAssignmentEntity> findByEmployeeId(UUID employeeId, Pageable pageable);
}
