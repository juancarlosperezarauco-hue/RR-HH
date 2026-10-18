package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class CancelEmployeeAssignmentUseCase {
    private final EmployeeAssignmentRepository assignments; private final EmployeePolicy employees;
    private final AssignmentPolicy policy; private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public CancelEmployeeAssignmentUseCase(EmployeeAssignmentRepository assignments, EmployeePolicy employees,
            AssignmentPolicy policy, EmployeeAudit audit, EmployeeMapper mapper) {
        this.assignments = assignments; this.employees = employees; this.policy = policy;
        this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeAssignmentResponse execute(UUID employeeId, UUID assignmentId) {
        employees.assertOperational(); employees.requireForUpdate(employeeId);
        EmployeeAssignment before = policy.require(employeeId, assignmentId);
        EmployeeAssignment saved = assignments.save(policy.cancelled(before));
        audit.record("EMPLOYEE_ASSIGNMENT", "CANCELLED", saved.id(), List.of("status"));
        return mapper.toResponse(saved);
    }
}
