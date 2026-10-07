package com.hrcoresystem.hrcore_api.modules.identity.auth.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.PasswordResetToken;
import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final TenantPasswordResetTokenJpaRepository jpaRepository;

    public PasswordResetTokenRepositoryAdapter(TenantPasswordResetTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        TenantPasswordResetTokenEntity entity = toEntity(passwordResetToken);
        TenantPasswordResetTokenEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
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

    private TenantPasswordResetTokenEntity toEntity(PasswordResetToken passwordResetToken) {
        TenantPasswordResetTokenEntity entity = new TenantPasswordResetTokenEntity();
        entity.setId(passwordResetToken.id());
        entity.setEmail(passwordResetToken.email());
        entity.setToken(passwordResetToken.token());
        entity.setExpiresAt(passwordResetToken.expiresAt());
        entity.setUsed(passwordResetToken.used());
        entity.setUsedAt(passwordResetToken.usedAt());
        return entity;
    }

    private PasswordResetToken toDomain(TenantPasswordResetTokenEntity entity) {
        return new PasswordResetToken(
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