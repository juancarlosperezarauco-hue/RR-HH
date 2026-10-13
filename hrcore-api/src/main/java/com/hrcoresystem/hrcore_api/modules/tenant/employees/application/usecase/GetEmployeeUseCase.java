package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeePolicy;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class GetEmployeeUseCase {
    private final EmployeePolicy policy; private final EmployeeMapper mapper;
    public GetEmployeeUseCase(EmployeePolicy policy, EmployeeMapper mapper) { this.policy = policy; this.mapper = mapper; }
    public EmployeeResponse execute(UUID id) {
        policy.assertOperational();
        return mapper.toResponse(policy.require(id));
    }
}
