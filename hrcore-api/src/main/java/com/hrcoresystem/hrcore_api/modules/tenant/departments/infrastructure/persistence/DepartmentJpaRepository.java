package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, UUID>, JpaSpecificationExecutor<DepartmentEntity> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    boolean existsByParentDepartmentIdAndStatus(UUID parentDepartmentId,
            com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentStatus status);
}
