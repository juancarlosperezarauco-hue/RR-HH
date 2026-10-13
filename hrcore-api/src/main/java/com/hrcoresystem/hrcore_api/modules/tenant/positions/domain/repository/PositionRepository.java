package com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository {
    Position save(Position position);
    Optional<Position> findById(UUID id);
    Optional<Position> findByIdForUpdate(UUID id);
    boolean existsByCodeOtherThan(String code, UUID id);
    Page<Position> findAll(PositionFilter filter, Pageable pageable);
    boolean hasCurrentAssignments(UUID positionId);
}
