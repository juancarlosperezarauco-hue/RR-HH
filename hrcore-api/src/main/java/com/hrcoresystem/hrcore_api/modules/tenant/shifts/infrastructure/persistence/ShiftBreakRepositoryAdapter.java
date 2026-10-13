package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftBreak;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.repository.ShiftBreakRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ShiftBreakRepositoryAdapter implements ShiftBreakRepository {
    private final ShiftBreakJpaRepository jpa;

    public ShiftBreakRepositoryAdapter(ShiftBreakJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ShiftBreak save(ShiftBreak shiftBreak) {
        ShiftBreakEntity entity = shiftBreak.id() == null
                ? new ShiftBreakEntity()
                : jpa.findByIdAndShiftId(shiftBreak.id(), shiftBreak.shiftId()).orElseThrow();
        entity.setShiftId(shiftBreak.shiftId());
        entity.setName(shiftBreak.name());
        entity.setStartTime(shiftBreak.startTime());
        entity.setEndTime(shiftBreak.endTime());
        entity.setDurationMinutes(shiftBreak.durationMinutes());
        entity.setPaid(shiftBreak.paid());
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<ShiftBreak> findByIdAndShiftId(UUID id, UUID shiftId) {
        return jpa.findByIdAndShiftId(id, shiftId).map(this::toDomain);
    }

    @Override
    public List<ShiftBreak> findAllByShiftId(UUID shiftId) {
        return jpa.findAllByShiftId(shiftId).stream().map(this::toDomain).toList();
    }

    @Override
    public Page<ShiftBreak> findAllByShiftId(UUID shiftId, Pageable pageable) {
        return jpa.findAllByShiftId(shiftId, pageable).map(this::toDomain);
    }

    @Override
    public void delete(ShiftBreak shiftBreak) {
        jpa.deleteById(shiftBreak.id());
        jpa.flush();
    }

    private ShiftBreak toDomain(ShiftBreakEntity entity) {
        return new ShiftBreak(entity.getId(), entity.getShiftId(), entity.getName(),
                entity.getStartTime(), entity.getEndTime(), entity.getDurationMinutes(),
                entity.isPaid(), entity.getCreatedAt());
    }
}
