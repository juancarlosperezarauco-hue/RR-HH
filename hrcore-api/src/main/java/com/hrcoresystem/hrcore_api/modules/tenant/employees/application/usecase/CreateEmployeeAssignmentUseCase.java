package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class CreateEmployeeAssignmentUseCase {
    private final EmployeeAssignmentRepository assignments; private final EmployeePolicy employees;
    private final AssignmentPolicy policy; private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public CreateEmployeeAssignmentUseCase(EmployeeAssignmentRepository assignments, EmployeePolicy employees,
                                           AssignmentPolicy policy, EmployeeAudit audit, EmployeeMapper mapper) {
        this.assignments = assignments; this.employees = employees; this.policy = policy;
        this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeAssignmentResponse execute(UUID employeeId, EmployeeAssignmentRequest request) {
        employees.assertOperational();
        Employee employee = employees.requireForUpdate(employeeId);
        EmployeeAssignment saved = assignments.save(policy.normalized(employee, request, null, employees.actorId()));
        audit.record("EMPLOYEE_ASSIGNMENT", "CREATED", saved.id(), List.of());
        return mapper.toResponse(saved);
    }
}
