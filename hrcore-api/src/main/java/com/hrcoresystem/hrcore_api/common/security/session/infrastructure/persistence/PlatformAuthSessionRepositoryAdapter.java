package com.hrcoresystem.hrcore_api.common.security.session.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.common.security.session.domain.model.AuthSession;
import com.hrcoresystem.hrcore_api.common.security.session.domain.repository.PlatformAuthSessionRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.net.InetAddress;

@Repository
public class PlatformAuthSessionRepositoryAdapter implements PlatformAuthSessionRepository {
    private final PlatformAuthSessionJpaRepository repository;

    public PlatformAuthSessionRepositoryAdapter(PlatformAuthSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession session) { return toDomain(repository.save(toEntity(session))); }
    @Override
    public Optional<AuthSession> findById(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override
    public Optional<AuthSession> findByRefreshTokenHash(String hash) { return repository.findByRefreshTokenHash(hash).map(this::toDomain); }
    @Override public List<AuthSession> findByUserId(UUID userId) { return repository.findBySuperadminIdOrderByCreatedAtDesc(userId).stream().map(this::toDomain).toList(); }
    @Override public int revokeOwnedSession(UUID sessionId, UUID userId) { return repository.revokeOwnedSession(sessionId, userId); }
    @Override public int revokeOtherSessions(UUID userId, UUID currentSessionId) { return repository.revokeOtherSessions(userId, currentSessionId); }

    private PlatformAuthSessionEntity toEntity(AuthSession s) {
        PlatformAuthSessionEntity e = new PlatformAuthSessionEntity();
        e.setId(s.id()); e.setSuperadminId(s.userId()); e.setTokenFamilyId(s.tokenFamilyId());
        e.setRefreshTokenHash(s.refreshTokenHash()); e.setRefreshGeneration(s.refreshGeneration());
        e.setCreatedAt(s.createdAt()); e.setExpiresAt(s.expiresAt()); e.setLastUsedAt(s.lastUsedAt());
        e.setLastRotatedAt(s.lastRotatedAt()); e.setRevokedAt(s.revokedAt()); e.setUserAgent(s.userAgent()); e.setIpAddress(parseIp(s.ipAddress()));
        return e;
    }

    private AuthSession toDomain(PlatformAuthSessionEntity e) {
        return new AuthSession(e.getId(), e.getSuperadminId(), e.getTokenFamilyId(), e.getRefreshTokenHash(), e.getRefreshGeneration(),
                e.getCreatedAt(), e.getExpiresAt(), e.getLastUsedAt(), e.getLastRotatedAt(), e.getRevokedAt(), e.getUserAgent(), e.getIpAddress() == null ? null : e.getIpAddress().getHostAddress());
    }
    private InetAddress parseIp(String value) { try { return value == null || value.isBlank() ? null : InetAddress.getByName(value); } catch (Exception e) { throw new IllegalArgumentException("Invalid session IP address", e); } }
}
