package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeEmergencyContactResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeePolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeEmergencyContactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ListEmployeeContactsUseCase {
    private final EmployeeEmergencyContactRepository contacts; private final EmployeePolicy employees; private final EmployeeMapper mapper;
    public ListEmployeeContactsUseCase(EmployeeEmergencyContactRepository contacts, EmployeePolicy employees, EmployeeMapper mapper) {
        this.contacts = contacts; this.employees = employees; this.mapper = mapper;
    }
    public Page<EmployeeEmergencyContactResponse> execute(UUID employeeId, Pageable pageable) {
        employees.assertOperational(); employees.require(employeeId);
        return contacts.findByEmployeeId(employeeId, pageable).map(mapper::toResponse);
    }
}
