package com.hrcoresystem.hrcore_api.common.security.session.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.net.InetAddress;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "platform_auth_sessions", schema = "public")
public class PlatformAuthSessionEntity {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Column(nullable = false) private UUID superadminId;
    @Column(nullable = false) private UUID tokenFamilyId;
    @Column(nullable = false, unique = true, length = 64) private String refreshTokenHash;
    @Column(nullable = false) private long refreshGeneration;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant lastRotatedAt;
    private Instant revokedAt;
    @Column(columnDefinition = "TEXT") private String userAgent;
    @JdbcTypeCode(SqlTypes.INET) @Column(columnDefinition = "INET") private InetAddress ipAddress;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSuperadminId() { return superadminId; }
    public void setSuperadminId(UUID value) { this.superadminId = value; }
    public UUID getTokenFamilyId() { return tokenFamilyId; }
    public void setTokenFamilyId(UUID value) { this.tokenFamilyId = value; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String value) { this.refreshTokenHash = value; }
    public long getRefreshGeneration() { return refreshGeneration; }
    public void setRefreshGeneration(long value) { this.refreshGeneration = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant value) { this.lastUsedAt = value; }
    public Instant getLastRotatedAt() { return lastRotatedAt; }
    public void setLastRotatedAt(Instant value) { this.lastRotatedAt = value; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant value) { this.revokedAt = value; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String value) { this.userAgent = value; }
    public InetAddress getIpAddress() { return ipAddress; }
    public void setIpAddress(InetAddress value) { this.ipAddress = value; }
}
