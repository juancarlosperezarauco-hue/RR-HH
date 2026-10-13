package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeEmergencyContactResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class GetEmployeeContactUseCase {
    private final EmployeePolicy employees; private final ContactPolicy policy; private final EmployeeMapper mapper;
    public GetEmployeeContactUseCase(EmployeePolicy employees, ContactPolicy policy, EmployeeMapper mapper) {
        this.employees = employees; this.policy = policy; this.mapper = mapper;
    }
    public EmployeeEmergencyContactResponse execute(UUID employeeId, UUID contactId) {
        employees.assertOperational(); employees.require(employeeId);
        return mapper.toResponse(policy.require(employeeId, contactId));
    }
}
