package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.DepartmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper.DepartmentMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.DepartmentPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListDepartmentsUseCase {
    private final DepartmentRepository departments;
    private final DepartmentPolicy policy;
    private final DepartmentMapper mapper;

    public ListDepartmentsUseCase(DepartmentRepository departments, DepartmentPolicy policy, DepartmentMapper mapper) {
        this.departments = departments; this.policy = policy; this.mapper = mapper;
    }

    public Page<DepartmentResponse> execute(DepartmentFilter filter, Pageable pageable) {
        policy.assertOperational();
        return departments.findAll(filter, pageable).map(mapper::toResponse);
    }
}
