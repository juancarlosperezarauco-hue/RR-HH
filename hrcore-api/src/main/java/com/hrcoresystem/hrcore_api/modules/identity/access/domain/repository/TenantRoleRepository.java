package com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRoleRepository {

    TenantRole save(TenantRole tenantRole);

    Optional<TenantRole> findById(UUID id);

    Optional<TenantRole> findByName(String name);

    List<TenantRole> findAll();

    List<TenantRole> findAllByIds(List<UUID> ids);

    boolean existsByName(String name);

    long countActiveCustomRoles();
}
