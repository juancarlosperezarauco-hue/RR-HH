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
public class UpdateEmployeeContactUseCase {
    private final EmployeeEmergencyContactRepository contacts; private final EmployeePolicy employees;
    private final ContactPolicy policy; private final EmployeeAudit audit; private final EmployeeMapper mapper;
    public UpdateEmployeeContactUseCase(EmployeeEmergencyContactRepository contacts, EmployeePolicy employees,
                                        ContactPolicy policy, EmployeeAudit audit, EmployeeMapper mapper) {
        this.contacts = contacts; this.employees = employees; this.policy = policy; this.audit = audit; this.mapper = mapper;
    }
    @Transactional
    public EmployeeEmergencyContactResponse execute(UUID employeeId, UUID contactId, EmployeeEmergencyContactRequest request) {
        employees.assertOperational(); employees.assertNotTerminated(employees.requireForUpdate(employeeId));
        EmployeeEmergencyContact before = policy.require(employeeId, contactId);
        EmployeeEmergencyContact after = policy.normalized(employeeId, request, before);
        List<String> changed = policy.changedFields(before, after);
        if (changed.isEmpty()) return mapper.toResponse(before);
        EmployeeEmergencyContact saved = contacts.save(after);
        audit.record("EMPLOYEE_CONTACT", "UPDATED", saved.id(), changed);
        return mapper.toResponse(saved);
    }
}
