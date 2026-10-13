package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarException;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkCalendarExceptionFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkCalendarExceptionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WorkCalendarExceptionRepositoryAdapter implements WorkCalendarExceptionRepository {
    private final WorkCalendarExceptionJpaRepository jpa;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public WorkCalendarExceptionRepositoryAdapter(WorkCalendarExceptionJpaRepository jpa, DataSource dataSource,
                                                  EntityManager entityManager) {
        this.jpa = jpa;
        this.jdbc = new JdbcTemplate(dataSource);
        this.entityManager = entityManager;
    }

    @Override
    public WorkCalendarException save(WorkCalendarException value) {
        WorkCalendarExceptionEntity entity = value.id() == null
                ? new WorkCalendarExceptionEntity()
                : jpa.findById(value.id()).orElseThrow();
        entity.setEmployeeId(value.employeeId());
        entity.setDepartmentId(value.departmentId());
        entity.setExceptionDate(value.exceptionDate());
        entity.setExceptionType(value.exceptionType());
        entity.setDescription(value.description());
        entity.setWorkRequired(value.workRequired());
        if (value.id() == null) entity.setCreatedBy(value.createdBy());
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<WorkCalendarException> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<WorkCalendarException> findByIdFresh(UUID id) {
        Optional<WorkCalendarExceptionEntity> found = jpa.findById(id);
        if (found.isEmpty()) return Optional.empty();
        try {
            entityManager.refresh(found.get());
            return Optional.of(toDomain(found.get()));
        } catch (EntityNotFoundException deleted) {
            return Optional.empty();
        }
    }

    @Override
    public Page<WorkCalendarException> findAll(WorkCalendarExceptionFilter filter, Pageable pageable) {
        Specification<WorkCalendarExceptionEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.employeeId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employeeId"), filter.employeeId()));
        if (filter.departmentId() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("departmentId"), filter.departmentId()));
        if (filter.fromDate() != null)
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("exceptionDate"), filter.fromDate()));
        if (filter.toDate() != null)
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("exceptionDate"), filter.toDate()));
        if (filter.exceptionType() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("exceptionType"), filter.exceptionType()));
        if (filter.workRequired() != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("workRequired"), filter.workRequired()));
        if (Boolean.TRUE.equals(filter.organizationOnly()))
            spec = spec.and((root, query, cb) -> cb.and(cb.isNull(root.get("employeeId")),
                    cb.isNull(root.get("departmentId"))));
        return jpa.findAll(spec, pageable).map(this::toDomain);
    }

    @Override
    public List<WorkCalendarException> findByScopeAndDate(UUID employeeId, UUID departmentId, LocalDate date) {
        List<WorkCalendarExceptionEntity> values = employeeId != null
                ? jpa.findByEmployeeIdAndExceptionDate(employeeId, date)
                : departmentId != null
                    ? jpa.findByDepartmentIdAndExceptionDate(departmentId, date)
                    : jpa.findByEmployeeIdIsNullAndDepartmentIdIsNullAndExceptionDate(date);
        return values.stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
        jpa.flush();
    }

    @Override
    public void lockScopeDate(UUID employeeId, UUID departmentId, LocalDate date) {
        String scope = employeeId != null ? "employee:" + employeeId
                : departmentId != null ? "department:" + departmentId : "organization";
        String key = "shifts:calendar:" + scope + ":" + date;
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(current_schema()), hashtext(?))",
                (ResultSetExtractor<Void>) rs -> null, key);
    }

    private WorkCalendarException toDomain(WorkCalendarExceptionEntity e) {
        return new WorkCalendarException(e.getId(), e.getEmployeeId(), e.getDepartmentId(),
                e.getExceptionDate(), e.getExceptionType(), e.getDescription(),
                e.isWorkRequired(), e.getCreatedBy(), e.getCreatedAt());
    }
}
