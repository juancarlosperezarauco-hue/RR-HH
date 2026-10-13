package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PositionRepositoryAdapter implements PositionRepository {
    private final PositionJpaRepository jpa;
    private final JdbcTemplate jdbc;

    public PositionRepositoryAdapter(PositionJpaRepository jpa, DataSource dataSource) {
        this.jpa = jpa;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public Position save(Position p) {
        PositionEntity e = p.id() == null ? new PositionEntity() : jpa.findById(p.id()).orElseThrow();
        e.setCode(p.code()); e.setName(p.name()); e.setDescription(p.description());
        e.setResponsibilities(p.responsibilities()); e.setOrganizationalLevel(p.organizationalLevel());
        e.setSalaryMin(p.salaryMin()); e.setSalaryMax(p.salaryMax()); e.setCurrency(p.currency());
        e.setStatus(p.status()); e.setCreatedBy(p.createdBy()); e.setUpdatedBy(p.updatedBy());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Position> findById(UUID id) { return jpa.findById(id).map(this::toDomain); }

    @Override
    public Optional<Position> findByIdForUpdate(UUID id) { return jpa.findByIdForUpdate(id).map(this::toDomain); }

    @Override
    public boolean existsByCodeOtherThan(String code, UUID id) {
        return id == null ? jpa.existsByCode(code) : jpa.existsByCodeAndIdNot(code, id);
    }

    @Override
    public Page<Position> findAll(PositionFilter filter, Pageable pageable) {
        Specification<PositionEntity> spec = (r, q, cb) -> cb.conjunction();
        if (filter.status() != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), filter.status()));
        if (filter.organizationalLevel() != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("organizationalLevel"), filter.organizationalLevel()));
        if (filter.name() != null && !filter.name().isBlank()) {
            String pattern = "%" + escapeLike(filter.name().trim().toLowerCase()) + "%";
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("name")), pattern, '\\'));
        }
        if (filter.code() != null && !filter.code().isBlank())
            spec = spec.and((r, q, cb) -> cb.equal(r.get("code"), filter.code().trim()));
        return jpa.findAll(spec, pageable).map(this::toDomain);
    }

    @Override
    public boolean hasCurrentAssignments(UUID positionId) {
        Boolean found = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM employee_assignments
                 WHERE position_id = ? AND start_date <= CURRENT_DATE
                   AND (end_date IS NULL OR end_date >= CURRENT_DATE))
                """, Boolean.class, positionId);
        return Boolean.TRUE.equals(found);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Position toDomain(PositionEntity e) {
        return new Position(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getResponsibilities(), e.getOrganizationalLevel(), e.getSalaryMin(), e.getSalaryMax(),
                e.getCurrency(), e.getStatus(), e.getCreatedBy(), e.getUpdatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
