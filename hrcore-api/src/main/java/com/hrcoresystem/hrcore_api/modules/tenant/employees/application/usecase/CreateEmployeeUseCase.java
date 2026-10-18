package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.BusinessSequenceService;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service.EmployeeFileProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CreateEmployeeUseCase {
    private final EmployeeRepository employees; private final EmployeePolicy policy;
    private final EmployeeAudit audit; private final EmployeeMapper mapper; private final BusinessSequenceService sequences; private final EmployeeFileProvisioningService fileProvisioning;
    @Autowired
    public CreateEmployeeUseCase(EmployeeRepository employees, EmployeePolicy policy,
                                 EmployeeAudit audit, EmployeeMapper mapper, BusinessSequenceService sequences, EmployeeFileProvisioningService fileProvisioning) {
        this.employees = employees; this.policy = policy; this.audit = audit; this.mapper = mapper; this.sequences = sequences; this.fileProvisioning = fileProvisioning;
    }
    @Transactional
    public EmployeeResponse execute(EmployeeRequest request) {
        policy.assertOperational();
        String employeeNumber = sequences.nextCode("employee");
        Employee saved = employees.save(policy.normalized(null, employeeNumber, request, null, policy.actorId()));
        fileProvisioning.provision(saved);
        audit.record("EMPLOYEE", "CREATED", saved.id(), List.of());
        return mapper.toResponse(saved);
    }
}
