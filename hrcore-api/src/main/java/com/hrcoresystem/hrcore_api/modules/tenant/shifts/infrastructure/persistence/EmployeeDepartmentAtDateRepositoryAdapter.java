package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.EmployeeDepartmentAtDateRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class EmployeeDepartmentAtDateRepositoryAdapter implements EmployeeDepartmentAtDateRepository {
    private final JdbcTemplate jdbc;

    public EmployeeDepartmentAtDateRepositoryAdapter(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public List<Placement> findAtDate(UUID employeeId, LocalDate date) {
        return jdbc.query("""
                SELECT department_id, assignment_type
                FROM employee_assignments
                WHERE employee_id = ? AND start_date <= ?
                  AND (end_date IS NULL OR end_date >= ?)
                """, (rs, rowNum) -> new Placement(
                rs.getObject("department_id", UUID.class),
                "PRIMARY".equals(rs.getString("assignment_type"))), employeeId, date, date);
    }
}
