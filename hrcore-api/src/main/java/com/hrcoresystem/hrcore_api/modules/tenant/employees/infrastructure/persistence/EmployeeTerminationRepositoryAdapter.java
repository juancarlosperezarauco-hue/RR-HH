package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeTermination;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeTerminationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
@Repository
public class EmployeeTerminationRepositoryAdapter implements EmployeeTerminationRepository {
    private final JdbcTemplate jdbc;
    public EmployeeTerminationRepositoryAdapter(DataSource dataSource) { this.jdbc = new JdbcTemplate(dataSource); }
    public EmployeeTermination save(EmployeeTermination t) {
        UUID id = t.id() == null ? UUID.randomUUID() : t.id();
        Instant created = t.createdAt() == null ? Instant.now() : t.createdAt();
        jdbc.update("INSERT INTO employee_terminations(id, employee_id, employment_period_id, effective_date, reason, performed_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, t.employeeId(), t.employmentPeriodId(), t.effectiveDate(), t.reason(), t.performedBy(), Timestamp.from(created));
        return new EmployeeTermination(id, t.employeeId(), t.employmentPeriodId(), t.effectiveDate(), t.reason(), t.performedBy(), created);
    }
    public Optional<EmployeeTermination> findByEmployeeId(UUID employeeId) {
        return jdbc.query("SELECT id, employee_id, employment_period_id, effective_date, reason, performed_by, created_at FROM employee_terminations WHERE employee_id = ? ORDER BY effective_date DESC, created_at DESC LIMIT 1",
                rs -> rs.next() ? Optional.of(new EmployeeTermination(rs.getObject("id", UUID.class), rs.getObject("employee_id", UUID.class), rs.getObject("employment_period_id", UUID.class),
                        rs.getObject("effective_date", java.time.LocalDate.class), rs.getString("reason"),
                        rs.getObject("performed_by", UUID.class), rs.getTimestamp("created_at").toInstant())) : Optional.empty(), employeeId);
    }
    public List<EmployeeTermination> findAllByEmployeeId(UUID employeeId) {
        return jdbc.query("SELECT id, employee_id, employment_period_id, effective_date, reason, performed_by, created_at FROM employee_terminations WHERE employee_id = ? ORDER BY effective_date DESC, created_at DESC", (rs, row) -> new EmployeeTermination(rs.getObject("id", UUID.class), rs.getObject("employee_id", UUID.class), rs.getObject("employment_period_id", UUID.class), rs.getObject("effective_date", java.time.LocalDate.class), rs.getString("reason"), rs.getObject("performed_by", UUID.class), rs.getTimestamp("created_at").toInstant()), employeeId);
    }
    public Optional<EmployeeTermination> findByEmploymentPeriodId(UUID periodId) {
        return jdbc.query("SELECT id, employee_id, employment_period_id, effective_date, reason, performed_by, created_at FROM employee_terminations WHERE employment_period_id = ?", rs -> rs.next() ? Optional.of(new EmployeeTermination(rs.getObject("id", UUID.class), rs.getObject("employee_id", UUID.class), rs.getObject("employment_period_id", UUID.class), rs.getObject("effective_date", java.time.LocalDate.class), rs.getString("reason"), rs.getObject("performed_by", UUID.class), rs.getTimestamp("created_at").toInstant())) : Optional.empty(), periodId);
    }
    public boolean existsByEmploymentPeriodId(UUID periodId) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM employee_terminations WHERE employment_period_id = ?)", Boolean.class, periodId)); }
}
