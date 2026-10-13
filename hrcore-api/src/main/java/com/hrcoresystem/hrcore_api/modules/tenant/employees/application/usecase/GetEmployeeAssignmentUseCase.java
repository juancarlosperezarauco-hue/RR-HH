package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class GetEmployeeAssignmentUseCase {
    private final EmployeePolicy employees; private final AssignmentPolicy policy; private final EmployeeMapper mapper;
    public GetEmployeeAssignmentUseCase(EmployeePolicy employees, AssignmentPolicy policy, EmployeeMapper mapper) {
        this.employees = employees; this.policy = policy; this.mapper = mapper;
    }
    public EmployeeAssignmentResponse execute(UUID employeeId, UUID assignmentId) {
        employees.assertOperational(); employees.require(employeeId);
        return mapper.toResponse(policy.require(employeeId, assignmentId));
    }
}
