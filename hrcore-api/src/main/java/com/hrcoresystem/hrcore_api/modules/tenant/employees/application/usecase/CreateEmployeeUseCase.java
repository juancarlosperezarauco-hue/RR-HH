package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CreateEmployeeUseCase {
    private final EmployeeRepository employees; private final EmployeePolicy policy;
    private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public CreateEmployeeUseCase(EmployeeRepository employees, EmployeePolicy policy,
                                 EmployeeAudit audit, EmployeeMapper mapper) {
        this.employees = employees; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeResponse execute(EmployeeRequest request) {
        policy.assertOperational();
        Employee saved = employees.save(policy.normalized(null, request, null, policy.actorId()));
        audit.record("EMPLOYEE", "CREATED", saved.id(), List.of());
        return mapper.toResponse(saved);
    }
}
