package com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.repository;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenant;
import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformTenantRepository {

    PlatformTenant save(PlatformTenant tenant);

    Optional<PlatformTenant> findById(UUID id);

    Optional<PlatformTenant> findBySlug(String slug);

    Optional<PlatformTenant> findByStripeCustomerId(String stripeCustomerId);

    List<PlatformTenant> findAll();

    List<PlatformTenant> searchTenants(String search, String name, String taxIdentifier, PlatformTenantStatus status, Boolean active, UUID planId);

    boolean existsBySlug(String slug);

    boolean existsBySchemaName(String schemaName);
}
