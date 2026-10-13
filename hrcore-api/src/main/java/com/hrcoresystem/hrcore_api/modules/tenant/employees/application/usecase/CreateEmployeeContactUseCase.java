package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeEmergencyContact;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeEmergencyContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class CreateEmployeeContactUseCase {
    private final EmployeeEmergencyContactRepository contacts; private final EmployeePolicy employees;
    private final ContactPolicy policy; private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public CreateEmployeeContactUseCase(EmployeeEmergencyContactRepository contacts, EmployeePolicy employees,
                                        ContactPolicy policy, EmployeeAudit audit, EmployeeMapper mapper) {
        this.contacts = contacts; this.employees = employees; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeEmergencyContactResponse execute(UUID employeeId, EmployeeEmergencyContactRequest request) {
        employees.assertOperational(); employees.assertNotTerminated(employees.requireForUpdate(employeeId));
        EmployeeEmergencyContact saved = contacts.save(policy.normalized(employeeId, request, null));
        audit.record("EMPLOYEE_CONTACT", "CREATED", saved.id(), List.of());
        return mapper.toResponse(saved);
    }
}
