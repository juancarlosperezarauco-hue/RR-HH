package com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalReportingRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalReportingRepository.OrganizationalEmployee;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class OrganizationalReportingService {
    private final OrganizationalReportingRepository repository;
    public OrganizationalReportingService(OrganizationalReportingRepository repository){this.repository=repository;}
    public List<OrganizationalEmployee> current(LocalDate date){return repository.findCurrent(date);}
    public Map<UUID,UUID> resolveSupervisors(List<OrganizationalEmployee> employees){
        Map<UUID,OrganizationalEmployee> byId=new HashMap<>(); employees.forEach(e->byId.put(e.employeeId(),e)); Map<UUID,UUID> result=new HashMap<>();
        for(OrganizationalEmployee employee:employees)result.put(employee.employeeId(),resolve(employee,byId)); validateNoCycles(result); return result;
    }
    public void validateNoCycles(LocalDate date){resolveSupervisors(current(date));}
    private void validateNoCycles(Map<UUID,UUID> graph){for(UUID start:graph.keySet()){Set<UUID> visiting=new HashSet<>();UUID cursor=start;while(cursor!=null){if(!visiting.add(cursor))throw conflict();cursor=graph.get(cursor);}}}
    private UUID resolve(OrganizationalEmployee employee,Map<UUID,OrganizationalEmployee> byId){
        if(employee.directSupervisorEmployeeId()!=null){if(employee.employeeId().equals(employee.directSupervisorEmployeeId())||!byId.containsKey(employee.directSupervisorEmployeeId()))throw conflict();return employee.directSupervisorEmployeeId();}
        Set<UUID> visited=new HashSet<>(); OrganizationalEmployee cursor=employee;
        while(cursor!=null&&visited.add(cursor.employeeId())){UUID candidate=cursor.managerEmployeeId();if(candidate!=null&&byId.containsKey(candidate)&&!candidate.equals(employee.employeeId()))return candidate;UUID parent=cursor.parentDepartmentId();cursor=parent==null?null:byId.values().stream().filter(e->e.departmentId().equals(parent)).findFirst().orElse(null);}
        return null;
    }
    private BusinessConflictException conflict(){return new BusinessConflictException("La relación de supervisión produciría una jerarquía circular");}
}
