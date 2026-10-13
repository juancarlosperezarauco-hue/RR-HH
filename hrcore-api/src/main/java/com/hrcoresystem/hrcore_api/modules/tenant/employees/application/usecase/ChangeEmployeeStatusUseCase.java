package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeEmployeeStatusUseCase {
    private final EmployeeRepository employees; private final EmployeePolicy policy;
    private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public ChangeEmployeeStatusUseCase(EmployeeRepository employees, EmployeePolicy policy,
                                       EmployeeAudit audit, EmployeeMapper mapper) {
        this.employees = employees; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeResponse execute(UUID id, EmploymentStatus status) {
        policy.assertOperational();
        Employee before = policy.requireForUpdate(id);
        policy.assertNotTerminated(before);
        if (status == null || status == EmploymentStatus.TERMINATED)
            throw new BusinessException("Unsupported employment status");
        if (before.employmentStatus() == status) return mapper.toResponse(before);
        Employee saved = employees.save(policy.changedStatus(before, status, policy.actorId()));
        audit.record("EMPLOYEE", "STATUS_UPDATED", id, List.of("employmentStatus"));
        return mapper.toResponse(saved);
    }
}
