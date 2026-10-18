package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.OrganizationalEmployeeSummaryResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
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
    public OrganizationalEmployeeQueryRepositoryAdapter(DataSource dataSource){jdbc=new JdbcTemplate(dataSource);}
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
        String cte="WITH RECURSIVE primary_assignments AS (SELECT ea.employee_id,ea.department_id,ea.position_id,ea.supervisor_employee_id FROM employee_assignments ea WHERE ea.assignment_type='PRIMARY' AND ea.assignment_status='ACTIVE' AND ea.start_date<=? AND (ea.end_date IS NULL OR ea.end_date>=?), chains AS (SELECT pa.employee_id,pa.department_id,pa.supervisor_employee_id,0 depth FROM primary_assignments pa UNION ALL SELECT c.employee_id,d.parent_department_id,c.supervisor_employee_id,c.depth+1 FROM chains c JOIN departments d ON d.id=c.department_id WHERE c.supervisor_employee_id IS NULL AND c.depth<100 AND c.department_id IS NOT NULL), effective AS (SELECT DISTINCT ON (c.employee_id) c.employee_id,COALESCE(c.supervisor_employee_id,d.manager_employee_id) supervisor_id FROM chains c LEFT JOIN departments d ON d.id=c.department_id WHERE c.supervisor_employee_id IS NOT NULL OR d.manager_employee_id IS NOT NULL ORDER BY c.employee_id,c.depth) ";
        String base=" FROM employees e JOIN primary_assignments ea ON ea.employee_id=e.id JOIN effective es ON es.employee_id=e.id WHERE es.supervisor_id=? AND e.id<>? AND "+ACTIVE;
        long total=jdbc.queryForObject(cte+"SELECT COUNT(*)"+base,Long.class,date,date,supervisorId,supervisorId);
        String sql=cte+"SELECT e.id,e.employee_number,e.first_name,e.middle_name,e.last_name,e.second_last_name,ea.department_id,ea.position_id,e.employment_status"+base+sort(page)+" LIMIT ? OFFSET ?";
        List<OrganizationalEmployeeSummaryResponse> rows=jdbc.query(sql,(rs,n)->map(rs),date,date,supervisorId,supervisorId,page.getPageSize(),page.getOffset());
        return new PageImpl<>(rows,page,total);
    }
    private String sort(Pageable page){
        if(page.getSort().isUnsorted())return " ORDER BY LOWER(e.last_name),LOWER(e.first_name),e.id";
        List<String> expressions=new ArrayList<>(); for(Sort.Order order:page.getSort()){String expression=switch(order.getProperty()){case "fullName"->"LOWER(CONCAT_WS(' ',e.first_name,e.middle_name,e.last_name,e.second_last_name))";case "employeeNumber"->"e.employee_number";case "employeeId","id"->"e.id";default->throw new BusinessConflictException("Ordenación no permitida: "+order.getProperty());};expressions.add(expression+" "+(order.isAscending()?"ASC":"DESC"));} expressions.add("e.id ASC"); return " ORDER BY "+String.join(",",expressions);
    }
    private OrganizationalEmployeeSummaryResponse map(java.sql.ResultSet rs)throws java.sql.SQLException{String full=String.join(" ",Arrays.asList(rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)).stream().filter(v->v!=null&&!v.isBlank()).toList());return new OrganizationalEmployeeSummaryResponse((UUID)rs.getObject(1),rs.getString(2),full,(UUID)rs.getObject(7),(UUID)rs.getObject(8),EmploymentStatus.valueOf(rs.getString(9)));}
}
