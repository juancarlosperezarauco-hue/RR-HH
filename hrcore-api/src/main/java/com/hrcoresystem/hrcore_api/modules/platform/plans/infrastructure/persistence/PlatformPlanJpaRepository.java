package com.hrcoresystem.hrcore_api.modules.platform.plans.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformPlanJpaRepository extends JpaRepository<PlatformPlanEntity, UUID> {

    boolean existsByCode(String code);

    Optional<PlatformPlanEntity> findByCode(String code);

    List<PlatformPlanEntity> findByActiveTrueAndPublicVisibleTrueOrderBySortOrderAscCodeAsc();
}
