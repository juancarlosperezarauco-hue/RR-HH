package com.hrcoresystem.hrcore_api.modules.platform.tenants.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformTenantJpaRepository extends JpaRepository<PlatformTenantEntity, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySchemaName(String schemaName);

    Optional<PlatformTenantEntity> findBySlug(String slug);

    Optional<PlatformTenantEntity> findByStripeCustomerId(String stripeCustomerId);
}
