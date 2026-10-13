package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DepartmentHistoryJpaRepository extends JpaRepository<DepartmentHistoryEntity, UUID> {
    Page<DepartmentHistoryEntity> findByDepartmentId(UUID departmentId, Pageable pageable);
}
