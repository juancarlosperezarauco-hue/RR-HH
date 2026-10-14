package com.hrcoresystem.hrcore_api.common.security.session;
import java.time.Instant;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.common.security.session.domain.model.AuthSession;
public record AuthSessionResponse(UUID id, boolean current, Instant createdAt, Instant lastUsedAt, Instant expiresAt, boolean revoked, String userAgent, String ipAddress) {
    public static AuthSessionResponse from(AuthSession s, UUID currentId) { return new AuthSessionResponse(s.id(), s.id().equals(currentId), s.createdAt(), s.lastUsedAt(), s.expiresAt(), s.revokedAt()!=null, s.userAgent(), s.ipAddress()); }
}
