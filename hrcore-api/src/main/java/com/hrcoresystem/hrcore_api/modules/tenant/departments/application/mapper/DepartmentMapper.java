package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.DepartmentHistoryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.DepartmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.Department;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentHistory;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {
    public DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.id(), d.code(), d.name(), d.description(), d.parentDepartmentId(),
                d.managerEmployeeId(), d.costCenterCode(), d.status(), d.createdBy(), d.updatedBy(),
                d.createdAt(), d.updatedAt());
    }

    public DepartmentHistoryResponse toHistoryResponse(DepartmentHistory h) {
        return new DepartmentHistoryResponse(h.id(), h.departmentId(), h.action(), h.previousData(),
                h.newData(), h.reason(), h.performedBy(), h.createdAt());
    }
}
