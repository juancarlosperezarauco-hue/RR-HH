package com.hrcoresystem.hrcore_api.modules.platform.tenants.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository.PlatformTenantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlatformTenantRepositoryAdapter implements PlatformTenantRepository {

    private final PlatformTenantJpaRepository jpaRepository;

    public PlatformTenantRepositoryAdapter(PlatformTenantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PlatformTenant save(PlatformTenant tenant) {
        PlatformTenantEntity entity = toEntity(tenant);
        PlatformTenantEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PlatformTenant> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<PlatformTenant> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug).map(this::toDomain);
    }

    @Override
    public Optional<PlatformTenant> findByStripeCustomerId(String stripeCustomerId) {
        return jpaRepository.findByStripeCustomerId(stripeCustomerId).map(this::toDomain);
    }

    @Override
    public List<PlatformTenant> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }

    @Override
    public boolean existsBySchemaName(String schemaName) {
        return jpaRepository.existsBySchemaName(schemaName);
    }

    private PlatformTenantEntity toEntity(PlatformTenant tenant) {
        PlatformTenantEntity entity = new PlatformTenantEntity();
        entity.setId(tenant.id());
        entity.setName(tenant.name());
        entity.setSlug(tenant.slug());
        entity.setSchemaName(tenant.schemaName());
        entity.setStatus(tenant.status());
        entity.setPlanId(tenant.planId());
        entity.setStripeCustomerId(tenant.stripeCustomerId());
        entity.setActive(tenant.active());
        return entity;
    }

    private PlatformTenant toDomain(PlatformTenantEntity entity) {
        return new PlatformTenant(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getSchemaName(),
                entity.getStatus(),
                entity.getPlanId(),
                entity.getStripeCustomerId(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
