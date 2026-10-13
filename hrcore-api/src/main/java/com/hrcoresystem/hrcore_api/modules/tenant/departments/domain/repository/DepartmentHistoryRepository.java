package com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface DepartmentHistoryRepository {
    DepartmentHistory save(DepartmentHistory history);
    Page<DepartmentHistory> findByDepartmentId(UUID departmentId, Pageable pageable);
}
