package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmploymentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.OrganizationalReportingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.*;

@Repository
public class OrganizationalReportingRepositoryAdapter implements OrganizationalReportingRepository {
    private final JdbcTemplate jdbc;
    public OrganizationalReportingRepositoryAdapter(DataSource dataSource){jdbc=new JdbcTemplate(dataSource);}
    @Override public List<OrganizationalEmployee> findCurrent(LocalDate date){
        String sql="SELECT e.id,e.employee_number,e.first_name,e.middle_name,e.last_name,e.second_last_name,e.employment_status,d.id,d.name,d.parent_department_id,d.manager_employee_id,p.id,p.name,ea.supervisor_employee_id,COALESCE(tu.active AND tu.status='ACTIVE',false) FROM employees e JOIN employee_assignments ea ON ea.employee_id=e.id AND ea.assignment_type='PRIMARY' AND ea.assignment_status='ACTIVE' AND ea.start_date<=? AND (ea.end_date IS NULL OR ea.end_date>=?) JOIN departments d ON d.id=ea.department_id JOIN positions p ON p.id=ea.position_id LEFT JOIN tenant_users tu ON tu.id=e.tenant_user_id WHERE e.employment_status IN ('ACTIVE','ON_LEAVE','SUSPENDED') ORDER BY e.id,ea.start_date DESC";
        List<OrganizationalEmployee> rows=jdbc.query(sql,(rs,n)->{String full=String.join(" ",Arrays.asList(rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)).stream().filter(v->v!=null&&!v.isBlank()).toList());return new OrganizationalEmployee((UUID)rs.getObject(1),rs.getString(2),full,EmploymentStatus.valueOf(rs.getString(7)),(UUID)rs.getObject(8),rs.getString(9),(UUID)rs.getObject(10),(UUID)rs.getObject(11),(UUID)rs.getObject(12),rs.getString(13),(UUID)rs.getObject(14),rs.getBoolean(15));},date,date);
        Set<UUID> seen=new HashSet<>(); for(OrganizationalEmployee row:rows)if(!seen.add(row.employeeId()))throw new com.hrcoresystem.hrcore_api.common.exception.BusinessConflictException("El empleado tiene asignaciones primarias superpuestas que requieren regularización"); return rows;
    }
}
