package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.OrganizationalEmployeeSummaryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.OrganizationalReportingService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalReportingRepository.OrganizationalEmployee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalEmployeeQueryRepository;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.*;

@Repository
public class OrganizationalEmployeeQueryRepositoryAdapter implements OrganizationalEmployeeQueryRepository {
    private static final String ACTIVE = "e.employment_status IN ('ACTIVE','ON_LEAVE','SUSPENDED')";
    private final JdbcTemplate jdbc;
    private final OrganizationalReportingService reporting;
    public OrganizationalEmployeeQueryRepositoryAdapter(DataSource dataSource, OrganizationalReportingService reporting){jdbc=new JdbcTemplate(dataSource);this.reporting=reporting;}
    @Override public Page<OrganizationalEmployeeSummaryResponse> findByDepartment(UUID id,LocalDate date,Pageable page){return query("ea.department_id = ?",id,date,page);}
    @Override public Page<OrganizationalEmployeeSummaryResponse> findByPosition(UUID id,LocalDate date,Pageable page){return query("ea.position_id = ?",id,date,page);}
    private Page<OrganizationalEmployeeSummaryResponse> query(String filter,UUID id,LocalDate date,Pageable page){
        ensureUnique(filter,id,date);
        String base=" FROM employees e JOIN employee_assignments ea ON ea.employee_id=e.id WHERE "+filter+" AND "+ACTIVE+" AND ea.assignment_type='PRIMARY' AND ea.assignment_status='ACTIVE' AND ea.start_date<=? AND (ea.end_date IS NULL OR ea.end_date>=?)";
        long total=jdbc.queryForObject("SELECT COUNT(*)"+base,Long.class,id,date,date);
        String sql="SELECT e.id,e.employee_number,e.first_name,e.middle_name,e.last_name,e.second_last_name,ea.department_id,ea.position_id,e.employment_status"+base+sort(page)+" LIMIT ? OFFSET ?";
        List<OrganizationalEmployeeSummaryResponse> rows=jdbc.query(sql,(rs,n)->map(rs),id,date,date,page.getPageSize(),page.getOffset());
        return new PageImpl<>(rows,page,total);
    }
    private void ensureUnique(String filter,UUID id,LocalDate date){Long conflicts=jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT ea.employee_id FROM employee_assignments ea WHERE "+filter+" AND ea.assignment_type='PRIMARY' AND ea.assignment_status='ACTIVE' AND ea.start_date<=? AND (ea.end_date IS NULL OR ea.end_date>=?) GROUP BY ea.employee_id HAVING COUNT(*)>1) x",Long.class,id,date,date);if(conflicts!=null&&conflicts>0)throw new BusinessConflictException("El empleado tiene asignaciones primarias superpuestas que requieren regularización");}
    @Override public Page<OrganizationalEmployeeSummaryResponse> findBySupervisor(UUID supervisorId,LocalDate date,Pageable page){
        Map<UUID, UUID> supervisors = reporting.resolveSupervisors(reporting.current(date));
        List<UUID> employeeIds = supervisors.entrySet().stream()
                .filter(entry -> supervisorId.equals(entry.getValue()))
                .map(Map.Entry::getKey).sorted().toList();
        if (employeeIds.isEmpty()) return Page.empty(page);
        String placeholders = String.join(",", Collections.nCopies(employeeIds.size(), "?"));
        String base = " FROM employees e JOIN employee_assignments ea ON ea.employee_id=e.id WHERE e.id IN (" + placeholders + ")"
                + " AND " + ACTIVE + " AND ea.assignment_type='PRIMARY' AND ea.assignment_status='ACTIVE'"
                + " AND ea.start_date<=? AND (ea.end_date IS NULL OR ea.end_date>=?)";
        List<Object> parameters = new ArrayList<>(employeeIds);
        parameters.add(date); parameters.add(date);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + base, Long.class, parameters.toArray());
        String sql = "SELECT e.id,e.employee_number,e.first_name,e.middle_name,e.last_name,e.second_last_name,ea.department_id,ea.position_id,e.employment_status"
                + base + sort(page) + " LIMIT ? OFFSET ?";
        parameters.add(page.getPageSize()); parameters.add(page.getOffset());
        List<OrganizationalEmployeeSummaryResponse> rows = jdbc.query(sql, (rs,n)->map(rs), parameters.toArray());
        return new PageImpl<>(rows,page,total == null ? 0L : total);
    }
    private String sort(Pageable page){
        if(page.getSort().isUnsorted())return " ORDER BY LOWER(e.last_name),LOWER(e.first_name),e.id";
        List<String> expressions=new ArrayList<>(); for(Sort.Order order:page.getSort()){String expression=switch(order.getProperty()){case "fullName"->"LOWER(CONCAT_WS(' ',e.first_name,e.middle_name,e.last_name,e.second_last_name))";case "employeeNumber"->"e.employee_number";case "employeeId","id"->"e.id";default->throw new BusinessConflictException("Ordenación no permitida: "+order.getProperty());};expressions.add(expression+" "+(order.isAscending()?"ASC":"DESC"));} expressions.add("e.id ASC"); return " ORDER BY "+String.join(",",expressions);
    }
    private OrganizationalEmployeeSummaryResponse map(java.sql.ResultSet rs)throws java.sql.SQLException{String full=String.join(" ",Arrays.asList(rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)).stream().filter(v->v!=null&&!v.isBlank()).toList());return new OrganizationalEmployeeSummaryResponse((UUID)rs.getObject(1),rs.getString(2),full,(UUID)rs.getObject(7),(UUID)rs.getObject(8),EmploymentStatus.valueOf(rs.getString(9)));}
}
