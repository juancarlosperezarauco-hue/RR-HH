package com.hrcoresystem.hrcore_api.modules.identity.auth.domain.repository;

import com.hrcoresystem.hrcore_api.modules.identity.auth.domain.model.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken passwordResetToken);

    Optional<PasswordResetToken> findByToken(String token);

    void invalidateAllByEmail(String email, Instant usedAt);

    void markUsed(String token, Instant usedAt);
}