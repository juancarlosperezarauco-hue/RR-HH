package com.hrcoresystem.hrcore_api.common.security.authorization.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.common.security.authorization.domain.model.AuthorizationCatalogState;
import com.hrcoresystem.hrcore_api.common.security.authorization.domain.repository.AuthorizationCatalogStateRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public class AuthorizationCatalogStateRepositoryAdapter implements AuthorizationCatalogStateRepository {
    private final AuthorizationCatalogStateJpaRepository repository;

    public AuthorizationCatalogStateRepositoryAdapter(AuthorizationCatalogStateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AuthorizationCatalogState> find() {
        return repository.findById((short) 1).map(this::toDomain);
    }

    @Override
    @Transactional
    public long incrementVersion() {
        repository.incrementCatalogVersion();
        return repository.findById((short) 1).orElseThrow().getCatalogVersion();
    }

    private AuthorizationCatalogState toDomain(AuthorizationCatalogStateEntity entity) {
        return new AuthorizationCatalogState(entity.getId(), entity.getCatalogVersion(), entity.getUpdatedAt());
    }
}
