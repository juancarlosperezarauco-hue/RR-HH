package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PositionRequirementJpaRepository extends JpaRepository<PositionRequirementEntity, UUID> {
    Optional<PositionRequirementEntity> findByIdAndPositionId(UUID id, UUID positionId);
    Page<PositionRequirementEntity> findByPositionId(UUID positionId, Pageable pageable);
}
