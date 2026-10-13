package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class UpdateEmployeeUseCase {
    private final EmployeeRepository employees; private final EmployeePolicy policy;
    private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public UpdateEmployeeUseCase(EmployeeRepository employees, EmployeePolicy policy,
                                 EmployeeAudit audit, EmployeeMapper mapper) {
        this.employees = employees; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeResponse execute(UUID id, EmployeeRequest request) {
        policy.assertOperational();
        Employee before = policy.requireForUpdate(id);
        Employee after = policy.normalized(id, request, before, policy.actorId());
        List<String> changed = policy.changedFields(before, after);
        if (changed.isEmpty()) return mapper.toResponse(before);
        Employee saved = employees.save(after);
        audit.record("EMPLOYEE", "UPDATED", saved.id(), changed);
        return mapper.toResponse(saved);
    }
}
