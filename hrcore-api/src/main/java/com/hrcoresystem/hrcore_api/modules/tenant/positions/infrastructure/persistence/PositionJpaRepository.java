package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PositionJpaRepository extends JpaRepository<PositionEntity, UUID>, JpaSpecificationExecutor<PositionEntity> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PositionEntity p where p.id = :id")
    Optional<PositionEntity> findByIdForUpdate(@Param("id") UUID id);
}
