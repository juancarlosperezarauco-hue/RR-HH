package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeEmergencyContactRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeEmergencyContact;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeEmergencyContactRepository;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class ContactPolicy {
    private final EmployeeEmergencyContactRepository contacts;
    private final EmployeePolicy employees;
    public ContactPolicy(EmployeeEmergencyContactRepository contacts, EmployeePolicy employees) {
        this.contacts = contacts; this.employees = employees;
    }
    public EmployeeEmergencyContact require(UUID employeeId, UUID contactId) {
        return contacts.findByIdAndEmployeeId(contactId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found: " + contactId));
    }
    public EmployeeEmergencyContact normalized(UUID employeeId, EmployeeEmergencyContactRequest r,
                                               EmployeeEmergencyContact before) {
        boolean primary = Boolean.TRUE.equals(r.primary());
        if (primary && contacts.existsOtherPrimary(employeeId, before == null ? null : before.id()))
            throw new BusinessException("Employee already has a primary emergency contact");
        return new EmployeeEmergencyContact(before == null ? null : before.id(), employeeId,
                employees.required(r.fullName(), 200, "fullName"),
                employees.optional(r.relationship(), 80, "relationship"),
                employees.phone(r.phone(), true, "phone"),
                employees.phone(r.alternativePhone(), false, "alternativePhone"),
                employees.optional(r.address(), 0, "address"), primary,
                before == null ? null : before.createdAt(), before == null ? null : before.updatedAt());
    }
    public List<String> changedFields(EmployeeEmergencyContact a, EmployeeEmergencyContact b) {
        List<String> fields = new ArrayList<>();
        changed(fields, "fullName", a.fullName(), b.fullName());
        changed(fields, "relationship", a.relationship(), b.relationship());
        changed(fields, "phone", a.phone(), b.phone());
        changed(fields, "alternativePhone", a.alternativePhone(), b.alternativePhone());
        changed(fields, "address", a.address(), b.address());
        changed(fields, "primary", a.primary(), b.primary());
        return fields;
    }
    private void changed(List<String> fields, String field, Object a, Object b) {
        if (!Objects.equals(a, b)) fields.add(field);
    }
}
