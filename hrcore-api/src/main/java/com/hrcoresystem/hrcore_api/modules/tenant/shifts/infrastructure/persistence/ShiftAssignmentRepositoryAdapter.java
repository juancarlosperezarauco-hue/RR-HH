package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.EmployeeShiftAssignment;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ShiftAssignmentRepositoryAdapter implements ShiftAssignmentRepository {
    private final ShiftAssignmentJpaRepository jpa;

    public ShiftAssignmentRepositoryAdapter(ShiftAssignmentJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public EmployeeShiftAssignment save(EmployeeShiftAssignment assignment) {
        ShiftAssignmentEntity entity = assignment.id() == null ? new ShiftAssignmentEntity()
                : jpa.findById(assignment.id()).orElseThrow();
        entity.setEmployeeId(assignment.employeeId());
        entity.setShiftId(assignment.shiftId());
        entity.setStartDate(assignment.startDate());
        entity.setEndDate(assignment.endDate());
        entity.setWeekdays(assignment.weekdays().toArray(Short[]::new));
        entity.setStatus(assignment.status());
        entity.setCreatedBy(assignment.createdBy());
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<EmployeeShiftAssignment> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<UUID> findEmployeeId(UUID id) { return jpa.findEmployeeId(id); }

    @Override
    public Page<EmployeeShiftAssignment> findAll(UUID employeeId, UUID shiftId,
            ShiftAssignmentStatus status, Pageable pageable) {
        Specification<ShiftAssignmentEntity> specification = (root, query, cb) -> cb.conjunction();
        if (employeeId != null)
            specification = specification.and((root, query, cb) -> cb.equal(root.get("employeeId"), employeeId));
        if (shiftId != null)
            specification = specification.and((root, query, cb) -> cb.equal(root.get("shiftId"), shiftId));
        if (status != null)
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        return jpa.findAll(specification, pageable).map(this::toDomain);
    }

    @Override
    public List<EmployeeShiftAssignment> findActiveOverlapping(UUID employeeId, UUID excludingId,
            LocalDate startDate, LocalDate endDate) {
        // A shift beginning on the last valid day can continue into the next day.
        LocalDate earliestOtherEnd = startDate.equals(LocalDate.MIN) ? startDate : startDate.minusDays(1);
        LocalDate latestOtherStart = endDate == null ? null
                : endDate.equals(LocalDate.MAX) ? endDate : endDate.plusDays(1);
        Specification<ShiftAssignmentEntity> specification = (root, query, cb) -> cb.and(
                cb.equal(root.get("employeeId"), employeeId),
                cb.equal(root.get("status"), ShiftAssignmentStatus.ACTIVE),
                cb.or(cb.isNull(root.get("endDate")), cb.greaterThanOrEqualTo(root.get("endDate"), earliestOtherEnd)));
        if (excludingId != null)
            specification = specification.and((root, query, cb) -> cb.notEqual(root.get("id"), excludingId));
        if (latestOtherStart != null)
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("startDate"), latestOtherStart));
        return jpa.findAll(specification).stream().map(this::toDomain).toList();
    }

    @Override
    public List<EmployeeShiftAssignment> findEffectiveOn(UUID employeeId, LocalDate date) {
        return jpa.findEffectiveOn(employeeId, date, (short) date.getDayOfWeek().getValue())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsAnyByShiftId(UUID shiftId) { return jpa.existsByShiftId(shiftId); }

    @Override
    public boolean existsActiveCurrentOrFutureByShiftId(UUID shiftId, LocalDate today) {
        return jpa.existsActiveCurrentOrFutureByShiftId(shiftId, today, ShiftAssignmentStatus.ACTIVE);
    }

    private EmployeeShiftAssignment toDomain(ShiftAssignmentEntity entity) {
        return new EmployeeShiftAssignment(entity.getId(), entity.getEmployeeId(), entity.getShiftId(),
                entity.getStartDate(), entity.getEndDate(), Arrays.asList(entity.getWeekdays()),
                entity.getStatus(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
