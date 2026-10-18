package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.Department;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalReportingRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalReportingRepository.OrganizationalEmployee;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrganizationalReportingService {
    private final OrganizationalReportingRepository repository;
    private final DepartmentRepository departments;

    public OrganizationalReportingService(OrganizationalReportingRepository repository, DepartmentRepository departments) {
        this.repository = repository;
        this.departments = departments;
    }

    public List<OrganizationalEmployee> current(LocalDate date) { return repository.findCurrent(date); }

    public Map<UUID, UUID> resolveSupervisors(List<OrganizationalEmployee> employees) {
        Map<UUID, OrganizationalEmployee> employeesById = employees.stream().collect(Collectors.toMap(
                OrganizationalEmployee::employeeId, Function.identity()));
        Map<UUID, Department> departmentsById = departments.findAllForTree(true).stream().collect(Collectors.toMap(
                Department::id, Function.identity()));
        Map<UUID, UUID> result = new HashMap<>();
        for (OrganizationalEmployee employee : employees) {
            result.put(employee.employeeId(), resolve(employee, employeesById, departmentsById));
        }
        validateNoCycles(result);
        return result;
    }

    public UUID resolveEffectiveSupervisor(UUID employeeId, LocalDate date) {
        return resolveSupervisors(current(date)).get(employeeId);
    }

    public void validateNoCycles(LocalDate date) { resolveSupervisors(current(date)); }

    private UUID resolve(OrganizationalEmployee employee, Map<UUID, OrganizationalEmployee> employeesById,
            Map<UUID, Department> departmentsById) {
        UUID employeeId = employee.employeeId();
        UUID directSupervisorId = employee.directSupervisorEmployeeId();
        if (directSupervisorId != null) {
            if (directSupervisorId.equals(employeeId)) throw conflict();
            if (isEligibleSupervisor(directSupervisorId, employeesById)) return directSupervisorId;
        }
        UUID departmentId = employee.departmentId();
        Set<UUID> visitedDepartments = new HashSet<>();
        while (departmentId != null && visitedDepartments.add(departmentId)) {
            Department department = departmentsById.get(departmentId);
            if (department == null) break;
            UUID managerId = department.managerEmployeeId();
            if (managerId != null && !managerId.equals(employeeId)
                    && isEligibleSupervisor(managerId, employeesById)) return managerId;
            departmentId = department.parentDepartmentId();
        }
        if (departmentId != null && visitedDepartments.contains(departmentId)) throw conflict();
        return null;
    }

    private boolean isEligibleSupervisor(UUID supervisorId, Map<UUID, OrganizationalEmployee> employeesById) {
        OrganizationalEmployee supervisor = employeesById.get(supervisorId);
        return supervisor != null && supervisor.status() == EmploymentStatus.ACTIVE && supervisor.accountActive();
    }

    private void validateNoCycles(Map<UUID, UUID> graph) {
        for (UUID start : graph.keySet()) {
            Set<UUID> visiting = new HashSet<>();
            UUID cursor = start;
            while (cursor != null) {
                if (!visiting.add(cursor)) throw conflict();
                cursor = graph.get(cursor);
            }
        }
    }

    private BusinessConflictException conflict() {
        return new BusinessConflictException("La relación de supervisión produciría una jerarquía circular");
    }
}
