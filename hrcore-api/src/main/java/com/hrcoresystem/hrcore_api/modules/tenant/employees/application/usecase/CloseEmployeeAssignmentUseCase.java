package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CloseEmployeeAssignmentUseCase {
    private final EmployeeAssignmentRepository assignments; private final EmployeePolicy employees;
    private final AssignmentPolicy policy; private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public CloseEmployeeAssignmentUseCase(EmployeeAssignmentRepository assignments, EmployeePolicy employees,
                                          AssignmentPolicy policy, EmployeeAudit audit, EmployeeMapper mapper) {
        this.assignments = assignments; this.employees = employees; this.policy = policy;
        this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeAssignmentResponse execute(UUID employeeId, UUID assignmentId, LocalDate endDate) {
        employees.assertOperational();
        Employee employee = employees.requireForUpdate(employeeId);
        employees.assertNotTerminated(employee);
        EmployeeAssignment before = policy.require(employeeId, assignmentId);
        EmployeeAssignment saved = assignments.save(policy.closed(before, endDate));
        audit.record("EMPLOYEE_ASSIGNMENT", "CLOSED", saved.id(), List.of("endDate"));
        return mapper.toResponse(saved);
    }
}
