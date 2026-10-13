package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftBreakJpaRepository extends JpaRepository<ShiftBreakEntity, UUID> {
    Optional<ShiftBreakEntity> findByIdAndShiftId(UUID id, UUID shiftId);
    List<ShiftBreakEntity> findAllByShiftId(UUID shiftId);
    Page<ShiftBreakEntity> findAllByShiftId(UUID shiftId, Pageable pageable);
}
