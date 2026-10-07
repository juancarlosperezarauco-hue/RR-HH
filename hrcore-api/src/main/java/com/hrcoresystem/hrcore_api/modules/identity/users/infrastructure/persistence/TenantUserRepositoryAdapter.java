package com.hrcoresystem.hrcore_api.modules.identity.users.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUser;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantUserRepositoryAdapter implements TenantUserRepository {

    private final TenantUserJpaRepository jpaRepository;

    public TenantUserRepositoryAdapter(TenantUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TenantUser save(TenantUser tenantUser) {
        TenantUserEntity entity = toEntity(tenantUser);
        TenantUserEntity saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TenantUser> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<TenantUser> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public List<TenantUser> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public long countActiveUsers() {
        return jpaRepository.countByActiveTrue();
    }

    @Override
    public long countActiveAndPendingUsers() {
        return jpaRepository.countByActiveTrueOrStatus(TenantUserStatus.PENDING);
    }

    private TenantUserEntity toEntity(TenantUser tenantUser) {
        TenantUserEntity entity = new TenantUserEntity();
        entity.setId(tenantUser.id());
        entity.setEmail(tenantUser.email());
        entity.setPasswordHash(tenantUser.passwordHash());
        entity.setFirstName(tenantUser.firstName());
        entity.setLastName(tenantUser.lastName());
        entity.setActive(tenantUser.active());
        entity.setStatus(tenantUser.status());
        return entity;
    }

    private TenantUser toDomain(TenantUserEntity entity) {
        return new TenantUser(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.isActive(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
