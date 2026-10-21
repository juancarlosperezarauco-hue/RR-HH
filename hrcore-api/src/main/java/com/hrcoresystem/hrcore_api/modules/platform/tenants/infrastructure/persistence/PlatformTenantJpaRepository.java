package com.hrcoresystem.hrcore_api.modules.platform.tenants.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.platform.tenants.domain.model.PlatformTenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformTenantJpaRepository extends JpaRepository<PlatformTenantEntity, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySchemaName(String schemaName);

    Optional<PlatformTenantEntity> findBySlug(String slug);

    Optional<PlatformTenantEntity> findByStripeCustomerId(String stripeCustomerId);

    @Query("""
        SELECT t FROM PlatformTenantEntity t
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :search, '%'))
               OR (t.legalName IS NOT NULL AND LOWER(t.legalName) LIKE LOWER(CONCAT('%', :search, '%')))
               OR (t.taxIdentifier IS NOT NULL AND LOWER(t.taxIdentifier) LIKE LOWER(CONCAT('%', :search, '%'))))
          AND (:name IS NULL OR :name = ''
               OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
               OR (t.legalName IS NOT NULL AND LOWER(t.legalName) LIKE LOWER(CONCAT('%', :name, '%'))))
          AND (:taxIdentifier IS NULL OR :taxIdentifier = ''
               OR (t.taxIdentifier IS NOT NULL AND LOWER(t.taxIdentifier) LIKE LOWER(CONCAT('%', :taxIdentifier, '%'))))
          AND (:status IS NULL OR t.status = :status)
          AND (:active IS NULL OR t.active = :active)
          AND (:planId IS NULL OR t.planId = :planId)
        ORDER BY t.createdAt DESC
    """)
    List<PlatformTenantEntity> searchTenants(
        @Param("search") String search,
        @Param("name") String name,
        @Param("taxIdentifier") String taxIdentifier,
        @Param("status") PlatformTenantStatus status,
        @Param("active") Boolean active,
        @Param("planId") UUID planId
    );
}
