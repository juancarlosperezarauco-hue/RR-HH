package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DepartmentRepositoryAdapter implements DepartmentRepository {
    private final DepartmentJpaRepository jpa;
    private final JdbcTemplate jdbc;

    public DepartmentRepositoryAdapter(DepartmentJpaRepository jpa, DataSource dataSource) {
        this.jpa = jpa;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public Department save(Department d) {
        DepartmentEntity e = new DepartmentEntity();
        e.setId(d.id());
        e.setCode(d.code());
        e.setName(d.name());
        e.setDescription(d.description());
        e.setParentDepartmentId(d.parentDepartmentId());
        e.setManagerEmployeeId(d.managerEmployeeId());
        e.setCostCenterCode(d.costCenterCode());
        e.setStatus(d.status());
        e.setCreatedBy(d.createdBy());
        e.setUpdatedBy(d.updatedBy());
        e.setCreatedAt(d.createdAt());
        e.setUpdatedAt(d.updatedAt());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Department> findById(UUID id) { return jpa.findById(id).map(this::toDomain); }

    @Override
    public boolean existsByCodeOtherThan(String code, UUID id) {
        return id == null ? jpa.existsByCode(code) : jpa.existsByCodeAndIdNot(code, id);
    }

    @Override
    public Page<Department> findAll(DepartmentFilter filter, Pageable pageable) {
        Specification<DepartmentEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.status() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), filter.status()));
        if (filter.name() != null && !filter.name().isBlank()) {
            String pattern = "%" + escapeLike(filter.name().trim().toLowerCase()) + "%";
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("name")), pattern, '\\'));
        }
        if (filter.code() != null && !filter.code().isBlank()) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("code"), filter.code().trim()));
        }
        if (filter.parentDepartmentId() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("parentDepartmentId"), filter.parentDepartmentId()));
        }
        return jpa.findAll(spec, pageable).map(this::toDomain);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public boolean existsActiveEmployee(UUID employeeId) {
        Boolean found = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM employees WHERE id = ? AND employment_status = 'ACTIVE')",
                Boolean.class, employeeId);
        return Boolean.TRUE.equals(found);
    }

    @Override
    public boolean hasActiveChildren(UUID departmentId) {
        return jpa.existsByParentDepartmentIdAndStatus(departmentId, DepartmentStatus.ACTIVE);
    }

    @Override
    public boolean hasCurrentAssignments(UUID departmentId) {
        Boolean found = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM employee_assignments
                 WHERE department_id = ? AND start_date <= CURRENT_DATE
                   AND (end_date IS NULL OR end_date >= CURRENT_DATE))
                """, Boolean.class, departmentId);
        return Boolean.TRUE.equals(found);
    }

    @Override
    public void lockHierarchy() {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(current_schema()), 24595)",
                (ResultSetExtractor<Void>) rs -> null);
    }

    private Department toDomain(DepartmentEntity e) {
        return new Department(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getParentDepartmentId(), e.getManagerEmployeeId(), e.getCostCenterCode(),
                e.getStatus(), e.getCreatedBy(), e.getUpdatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
