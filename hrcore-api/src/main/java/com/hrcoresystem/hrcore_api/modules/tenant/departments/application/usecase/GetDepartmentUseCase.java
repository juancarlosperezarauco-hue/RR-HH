package com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.DepartmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper.DepartmentMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.DepartmentPolicy;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class GetDepartmentUseCase {
    private final DepartmentPolicy policy;
    private final DepartmentMapper mapper;

    public GetDepartmentUseCase(DepartmentPolicy policy, DepartmentMapper mapper) {
        this.policy = policy; this.mapper = mapper;
    }

    public DepartmentResponse execute(UUID id) {
        policy.assertOperational();
        return mapper.toResponse(policy.require(id));
    }
}
