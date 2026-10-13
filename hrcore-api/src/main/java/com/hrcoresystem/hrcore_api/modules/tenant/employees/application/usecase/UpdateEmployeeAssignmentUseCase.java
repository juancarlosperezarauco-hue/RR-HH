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
public class UpdateEmployeeAssignmentUseCase {
    private final EmployeeAssignmentRepository assignments; private final EmployeePolicy employees;
    private final AssignmentPolicy policy; private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public UpdateEmployeeAssignmentUseCase(EmployeeAssignmentRepository assignments, EmployeePolicy employees,
                                           AssignmentPolicy policy, EmployeeAudit audit, EmployeeMapper mapper) {
        this.assignments = assignments; this.employees = employees; this.policy = policy;
        this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeAssignmentResponse execute(UUID employeeId, UUID assignmentId, EmployeeAssignmentRequest request) {
        employees.assertOperational();
        Employee employee = employees.requireForUpdate(employeeId);
        EmployeeAssignment before = policy.require(employeeId, assignmentId);
        EmployeeAssignment after = policy.normalized(employee, request, before, employees.actorId());
        List<String> changed = policy.changedFields(before, after);
        if (changed.isEmpty()) return mapper.toResponse(before);
        EmployeeAssignment saved = assignments.save(after);
        audit.record("EMPLOYEE_ASSIGNMENT", "UPDATED", saved.id(), changed);
        return mapper.toResponse(saved);
    }
}
