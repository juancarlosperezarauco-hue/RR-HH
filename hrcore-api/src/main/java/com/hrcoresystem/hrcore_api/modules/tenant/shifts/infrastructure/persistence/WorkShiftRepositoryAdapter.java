package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShift;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.WorkShiftFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.WorkShiftRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class WorkShiftRepositoryAdapter implements WorkShiftRepository {
    private final WorkShiftJpaRepository jpa;

    public WorkShiftRepositoryAdapter(WorkShiftJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public WorkShift save(WorkShift shift) {
        WorkShiftEntity entity = shift.id() == null
                ? new WorkShiftEntity()
                : jpa.findById(shift.id()).orElseThrow();
        entity.setCode(shift.code());
        entity.setName(shift.name());
        entity.setDescription(shift.description());
        entity.setStartTime(shift.startTime());
        entity.setEndTime(shift.endTime());
        entity.setCrossesMidnight(shift.crossesMidnight());
        entity.setLateToleranceMinutes(shift.lateToleranceMinutes());
        entity.setEarlyDepartureToleranceMinutes(shift.earlyDepartureToleranceMinutes());
        entity.setStatus(shift.status());
        entity.setCreatedBy(shift.createdBy());
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<WorkShift> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<WorkShift> lockById(UUID id) {
        return jpa.lockById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByCodeOtherThan(String code, UUID id) {
        return id == null ? jpa.existsByCode(code) : jpa.existsByCodeAndIdNot(code, id);
    }

    @Override
    public Page<WorkShift> findAll(WorkShiftFilter filter, Pageable pageable) {
        Specification<WorkShiftEntity> specification = (root, query, criteria) -> criteria.conjunction();
        if (filter.status() != null) {
            specification = specification.and((root, query, criteria) ->
                    criteria.equal(root.get("status"), filter.status()));
        }
        if (filter.code() != null && !filter.code().isBlank()) {
            String code = filter.code().trim();
            specification = specification.and((root, query, criteria) ->
                    criteria.equal(root.get("code"), code));
        }
        if (filter.name() != null && !filter.name().isBlank()) {
            String pattern = "%" + escapeLike(filter.name().trim().toLowerCase()) + "%";
            specification = specification.and((root, query, criteria) ->
                    criteria.like(criteria.lower(root.get("name")), pattern, '\\'));
        }
        return jpa.findAll(specification, pageable).map(this::toDomain);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private WorkShift toDomain(WorkShiftEntity entity) {
        return new WorkShift(entity.getId(), entity.getCode(), entity.getName(),
                entity.getDescription(), entity.getStartTime(), entity.getEndTime(),
                entity.isCrossesMidnight(), entity.getLateToleranceMinutes(),
                entity.getEarlyDepartureToleranceMinutes(), entity.getStatus(),
                entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
