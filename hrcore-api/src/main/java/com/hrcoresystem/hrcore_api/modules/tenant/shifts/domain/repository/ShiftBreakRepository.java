package com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftBreak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ShiftBreakRepository {
    ShiftBreak save(ShiftBreak shiftBreak);
    Optional<ShiftBreak> findByIdAndShiftId(UUID id, UUID shiftId);
    List<ShiftBreak> findAllByShiftId(UUID shiftId);
    Page<ShiftBreak> findAllByShiftId(UUID shiftId, Pageable pageable);
    Map<UUID, Integer> sumUnpaidMinutesByShiftIds(List<UUID> shiftIds);
    void delete(ShiftBreak shiftBreak);
}
