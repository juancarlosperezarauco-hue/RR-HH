package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmployeeAssignmentRepositoryAdapter implements EmployeeAssignmentRepository {
    private final EmployeeAssignmentJpaRepository jpa;
    private final JdbcTemplate jdbc;
    public EmployeeAssignmentRepositoryAdapter(EmployeeAssignmentJpaRepository jpa, DataSource dataSource) {
        this.jpa = jpa; this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public EmployeeAssignment save(EmployeeAssignment a) {
        EmployeeAssignmentEntity e = a.id() == null ? new EmployeeAssignmentEntity()
                : jpa.findByIdAndEmployeeId(a.id(), a.employeeId()).orElseThrow();
        e.setEmployeeId(a.employeeId()); e.setDepartmentId(a.departmentId()); e.setPositionId(a.positionId());
        e.setSupervisorEmployeeId(a.supervisorEmployeeId()); e.setStartDate(a.startDate());
        e.setEndDate(a.endDate()); e.setAssignmentType(a.assignmentType()); e.setReason(a.reason());
        e.setCreatedBy(a.createdBy());
        return toDomain(jpa.saveAndFlush(e));
    }
    @Override public Optional<EmployeeAssignment> findByIdAndEmployeeId(UUID id, UUID employeeId) {
        return jpa.findByIdAndEmployeeId(id, employeeId).map(this::toDomain);
    }
    @Override public Page<EmployeeAssignment> findByEmployeeId(UUID employeeId, Pageable pageable) {
        return jpa.findByEmployeeId(employeeId, pageable).map(this::toDomain);
    }
    @Override public boolean hasOpenOrFutureAssignments(UUID employeeId) {
        Boolean found = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM employee_assignments
                 WHERE employee_id = ? AND (end_date IS NULL OR end_date >= CURRENT_DATE))
                """, Boolean.class, employeeId);
        return Boolean.TRUE.equals(found);
    }
    @Override public boolean hasOverlappingPrimary(UUID employeeId, UUID exceptId,
                                                     LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT EXISTS (SELECT 1 FROM employee_assignments
                 WHERE employee_id = ? AND assignment_type = 'PRIMARY'
                   AND (end_date IS NULL OR end_date >= ?)
                """);
        List<Object> args = new ArrayList<>(List.of(employeeId, startDate));
        if (endDate != null) { sql.append(" AND start_date <= ?"); args.add(endDate); }
        if (exceptId != null) { sql.append(" AND id <> ?"); args.add(exceptId); }
        sql.append(')');
        Boolean found = jdbc.queryForObject(sql.toString(), Boolean.class, args.toArray());
        return Boolean.TRUE.equals(found);
    }
    @Override public Optional<LocalDate> earliestStartDate(UUID employeeId) {
        LocalDate earliest = jdbc.queryForObject("SELECT MIN(start_date) FROM employee_assignments WHERE employee_id = ?",
                (rs, rowNum) -> rs.getObject(1, LocalDate.class), employeeId);
        return Optional.ofNullable(earliest);
    }

    private EmployeeAssignment toDomain(EmployeeAssignmentEntity e) {
        return new EmployeeAssignment(e.getId(), e.getEmployeeId(), e.getDepartmentId(), e.getPositionId(),
                e.getSupervisorEmployeeId(), e.getStartDate(), e.getEndDate(), e.getAssignmentType(),
                e.getReason(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
