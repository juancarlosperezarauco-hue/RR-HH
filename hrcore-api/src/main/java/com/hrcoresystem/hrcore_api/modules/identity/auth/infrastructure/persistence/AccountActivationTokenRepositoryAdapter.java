package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.AccountActivationToken;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.AccountActivationTokenRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class AccountActivationTokenRepositoryAdapter implements AccountActivationTokenRepository {

    private final TenantAccountActivationTokenJpaRepository jpaRepository;

    public AccountActivationTokenRepositoryAdapter(TenantAccountActivationTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AccountActivationToken save(AccountActivationToken accountActivationToken) {
        TenantAccountActivationTokenEntity entity = toEntity(accountActivationToken);
        TenantAccountActivationTokenEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AccountActivationToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public void invalidateAllByEmail(String email, Instant usedAt) {
        jpaRepository.invalidateAllByEmail(email, usedAt);
    }

    @Override
    public void markUsed(String token, Instant usedAt) {
        jpaRepository.markUsed(token, usedAt);
    }

    private TenantAccountActivationTokenEntity toEntity(AccountActivationToken accountActivationToken) {
        TenantAccountActivationTokenEntity entity = new TenantAccountActivationTokenEntity();
        entity.setId(accountActivationToken.id());
        entity.setEmail(accountActivationToken.email());
        entity.setToken(accountActivationToken.token());
        entity.setExpiresAt(accountActivationToken.expiresAt());
        entity.setUsed(accountActivationToken.used());
        entity.setUsedAt(accountActivationToken.usedAt());
        return entity;
    }

    private AccountActivationToken toDomain(TenantAccountActivationTokenEntity entity) {
        return new AccountActivationToken(
                entity.getId(),
                entity.getEmail(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.isUsed(),
                entity.getUsedAt(),
                entity.getCreatedAt()
        );
    }
}
