package com.hrcoresystem.hrcore_api.modules.identity.users.application.mapper;

import com.hrcoresystem.hrcore_api.modules.identity.access.application.dto.AssignedRoleResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.application.dto.TenantUserResponse;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeEntity;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence.EmployeeJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantUserMapper {

    private final EmployeeJpaRepository employeeJpaRepository;

    public TenantUserMapper(@Autowired(required = false) EmployeeJpaRepository employeeJpaRepository) {
        this.employeeJpaRepository = employeeJpaRepository;
    }

    public TenantUserMapper() {
        this(null);
    }

    public TenantUserResponse toResponse(TenantUser tenantUser) {
        return toResponse(tenantUser, List.of());
    }

    public TenantUserResponse toResponse(TenantUser tenantUser, List<AssignedRoleResponse> roles) {
        String displayName = ((tenantUser.firstName() != null ? tenantUser.firstName() : "") + " "
                + (tenantUser.lastName() != null ? tenantUser.lastName() : "")).trim();
        if (displayName.isEmpty()) {
            displayName = tenantUser.email();
        }

        UUID employeeId = null;
        String employeeNumber = null;
        String employeeFullName = null;

        if (employeeJpaRepository != null) {
            Optional<EmployeeEntity> empOpt = employeeJpaRepository.findByTenantUserId(tenantUser.id());
            if (empOpt.isPresent()) {
                EmployeeEntity emp = empOpt.get();
                employeeId = emp.getId();
                employeeNumber = emp.getEmployeeNumber();
                String empName = ((emp.getFirstName() != null ? emp.getFirstName() : "") + " "
                        + (emp.getLastName() != null ? emp.getLastName() : "")).trim();
                employeeFullName = empName.isEmpty() ? null : empName;
            }
        }

        return new TenantUserResponse(
                tenantUser.id(),
                tenantUser.email(),
                tenantUser.firstName(),
                tenantUser.lastName(),
                displayName,
                tenantUser.active(),
                tenantUser.status().name(),
                tenantUser.createdAt(),
                tenantUser.updatedAt(),
                roles,
                employeeId,
                employeeNumber,
                employeeFullName
        );
    }
}
