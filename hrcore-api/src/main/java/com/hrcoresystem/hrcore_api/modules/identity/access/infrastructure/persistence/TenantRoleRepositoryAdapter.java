package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.TenantRole;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.repository.TenantRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantRoleRepositoryAdapter implements TenantRoleRepository {

    private final TenantRoleJpaRepository jpaRepository;

    public TenantRoleRepositoryAdapter(TenantRoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TenantRole save(TenantRole tenantRole) {
        TenantRoleEntity entity = toEntity(tenantRole);
        TenantRoleEntity saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TenantRole> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<TenantRole> findByName(String name) {
        return jpaRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public Optional<TenantRole> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }

    @Override
    public List<TenantRole> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TenantRole> findAllByIds(List<UUID> ids) {
        return jpaRepository.findAllById(ids)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public long countActiveCustomRoles() {
        return jpaRepository.countByActiveTrueAndSystemRoleFalse();
    }

    private TenantRoleEntity toEntity(TenantRole tenantRole) {
        TenantRoleEntity entity = new TenantRoleEntity();
        entity.setId(tenantRole.id());
        entity.setCode(tenantRole.code());
        entity.setName(tenantRole.name());
        entity.setDescription(tenantRole.description());
        entity.setSystemRole(tenantRole.systemRole());
        entity.setActive(tenantRole.active());
        return entity;
    }

    private TenantRole toDomain(TenantRoleEntity entity) {
        return new TenantRole(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isSystemRole(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
