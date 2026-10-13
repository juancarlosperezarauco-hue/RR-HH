package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeEmergencyContactJpaRepository extends JpaRepository<EmployeeEmergencyContactEntity, UUID> {
    Optional<EmployeeEmergencyContactEntity> findByIdAndEmployeeId(UUID id, UUID employeeId);
    Page<EmployeeEmergencyContactEntity> findByEmployeeId(UUID employeeId, Pageable pageable);
    boolean existsByEmployeeIdAndPrimaryTrue(UUID employeeId);
    boolean existsByEmployeeIdAndPrimaryTrueAndIdNot(UUID employeeId, UUID id);
}
