package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CurrentEmployeeResolver {
    private final EmployeeRepository employees;
    private final SecurityContextFacade security;
    public CurrentEmployeeResolver(EmployeeRepository employees, SecurityContextFacade security) { this.employees = employees; this.security = security; }
    public Employee require() {
        UUID user = security.getCurrentUserId();
        if (user == null) throw new AccessDeniedException("La cuenta actual no está vinculada a un empleado");
        return employees.findByTenantUserId(user).orElseThrow(() -> new AccessDeniedException("La cuenta actual no está vinculada a un empleado"));
    }
    public UUID id() { return require().id(); }
}
