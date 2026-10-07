package com.hrcoresystem.hrcore_api.modules.platform.plans.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.model.PlatformPlan;
import com.hrcoresystem.hrcore_api.modules.platform.plans.domain.repository.PlatformPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlatformPlanRepositoryAdapter implements PlatformPlanRepository {

    private final PlatformPlanJpaRepository jpaRepository;

    public PlatformPlanRepositoryAdapter(PlatformPlanJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlatformPlan save(PlatformPlan plan) {
        PlatformPlanEntity entity = toEntity(plan);
        PlatformPlanEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PlatformPlan> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PlatformPlan> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public List<PlatformPlan> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PlatformPlan> findActivePublicPlans() {
        return jpaRepository.findByActiveTrueAndPublicVisibleTrueOrderBySortOrderAscCodeAsc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    private PlatformPlanEntity toEntity(PlatformPlan plan) {
        PlatformPlanEntity entity = new PlatformPlanEntity();
        entity.setId(plan.id());
        entity.setCode(plan.code());
        entity.setName(plan.name());
        entity.setDescription(plan.description());
        entity.setMaxUsers(plan.maxUsers());
        entity.setMaxRoles(plan.maxRoles());
        entity.setPlanType(plan.planType());
        entity.setTrialDays(plan.trialDays());
        entity.setMonthlyAmount(plan.monthlyAmount());
        entity.setYearlyAmount(plan.yearlyAmount());
        entity.setCurrency(plan.currency());
        entity.setPublicVisible(plan.publicVisible());
        entity.setSortOrder(plan.sortOrder());
        entity.setActive(plan.active());
        return entity;
    }

    private PlatformPlan toDomain(PlatformPlanEntity entity) {
        return new PlatformPlan(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getMaxUsers(),
                entity.getMaxRoles(),
                entity.getPlanType(),
                entity.getTrialDays(),
                entity.getMonthlyAmount(),
                entity.getYearlyAmount(),
                entity.getCurrency(),
                entity.isPublicVisible(),
                entity.getSortOrder(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
