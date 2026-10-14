package com.hrcoresystem.hrcore_api.common.security.authorization.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.model.TenantAuthorizationState;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.TenantAuthorizationStateRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantAuthorizationStateRepositoryAdapter implements TenantAuthorizationStateRepository {
    private final TenantAuthorizationStateJpaRepository repository;

    public TenantAuthorizationStateRepositoryAdapter(TenantAuthorizationStateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TenantAuthorizationState> find() {
        return repository.findById((short) 1).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<TenantAuthorizationState> findForUpdate() {
        return repository.findLockedById((short) 1).map(this::toDomain);
    }

    @Override
    @Transactional
    public long incrementVersion() {
        repository.incrementAuthorizationVersion();
        return repository.findById((short) 1).orElseThrow().getAuthorizationVersion();
    }

    @Override
    public UUID ownerUserId() {
        return repository.findById((short) 1).map(TenantAuthorizationStateEntity::getOwnerUserId).orElse(null);
    }

    private TenantAuthorizationState toDomain(TenantAuthorizationStateEntity entity) {
        return new TenantAuthorizationState(entity.getId(), entity.getOwnerUserId(), entity.getAuthorizationVersion(), entity.getUpdatedAt());
    }
}
