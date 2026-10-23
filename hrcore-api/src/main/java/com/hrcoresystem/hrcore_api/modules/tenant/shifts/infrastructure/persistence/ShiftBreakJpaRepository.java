package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftBreakJpaRepository extends JpaRepository<ShiftBreakEntity, UUID> {
    Optional<ShiftBreakEntity> findByIdAndShiftId(UUID id, UUID shiftId);
    List<ShiftBreakEntity> findAllByShiftId(UUID shiftId);
    Page<ShiftBreakEntity> findAllByShiftId(UUID shiftId, Pageable pageable);

    @Query("select b.shiftId, coalesce(sum(b.durationMinutes), 0) from ShiftBreakEntity b "
            + "where b.shiftId in :shiftIds and b.paid = false group by b.shiftId")
    List<Object[]> sumUnpaidMinutesByShiftIds(@Param("shiftIds") List<UUID> shiftIds);
}
