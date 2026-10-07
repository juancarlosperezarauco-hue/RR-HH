package com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformPlanRepository {

    PlatformPlan save(PlatformPlan plan);

    Optional<PlatformPlan> findById(UUID id);

    Optional<PlatformPlan> findByCode(String code);

    List<PlatformPlan> findAll();

    List<PlatformPlan> findActivePublicPlans();

    boolean existsByCode(String code);
}
