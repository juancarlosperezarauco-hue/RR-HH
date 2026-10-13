package com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.PositionRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface PositionRequirementRepository {
    PositionRequirement save(PositionRequirement requirement);
    Optional<PositionRequirement> findByIdAndPositionId(UUID id, UUID positionId);
    Page<PositionRequirement> findByPositionId(UUID positionId, Pageable pageable);
    void delete(PositionRequirement requirement);
}
