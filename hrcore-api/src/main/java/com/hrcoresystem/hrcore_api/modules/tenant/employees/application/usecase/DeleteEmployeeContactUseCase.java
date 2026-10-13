package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeEmergencyContact;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeEmergencyContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class DeleteEmployeeContactUseCase {
    private final EmployeeEmergencyContactRepository contacts; private final EmployeePolicy employees;
    private final ContactPolicy policy; private final EmployeeAudit audit;
    public DeleteEmployeeContactUseCase(EmployeeEmergencyContactRepository contacts, EmployeePolicy employees,
                                        ContactPolicy policy, EmployeeAudit audit) {
        this.contacts = contacts; this.employees = employees; this.policy = policy; this.audit = audit;
    }
    @Transactional
    public void execute(UUID employeeId, UUID contactId) {
        employees.assertOperational(); employees.assertNotTerminated(employees.requireForUpdate(employeeId));
        EmployeeEmergencyContact before = policy.require(employeeId, contactId);
        contacts.delete(before);
        audit.record("EMPLOYEE_CONTACT", "DELETED", before.id(), List.of());
    }
}
