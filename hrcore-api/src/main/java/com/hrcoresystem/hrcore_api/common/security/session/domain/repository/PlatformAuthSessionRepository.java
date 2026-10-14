package com.hrcoresystem.hrcore_api.common.security.session.domain.repository;

import com.hrcoresystem.hrcore_api.common.security.session.domain.model.AuthSession;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PlatformAuthSessionRepository {
    AuthSession save(AuthSession session);
    Optional<AuthSession> findById(UUID id);
    Optional<AuthSession> findByRefreshTokenHash(String hash);
    List<AuthSession> findByUserId(UUID userId);
    int revokeOwnedSession(UUID sessionId, UUID userId);
    int revokeOtherSessions(UUID userId, UUID currentSessionId);
}
