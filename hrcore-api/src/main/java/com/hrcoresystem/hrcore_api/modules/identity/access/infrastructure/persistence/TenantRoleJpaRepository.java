package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRoleJpaRepository extends JpaRepository<TenantRoleEntity, UUID> {

    Optional<TenantRoleEntity> findByName(String name);

    Optional<TenantRoleEntity> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    long countByActiveTrueAndSystemRoleFalse();
}
