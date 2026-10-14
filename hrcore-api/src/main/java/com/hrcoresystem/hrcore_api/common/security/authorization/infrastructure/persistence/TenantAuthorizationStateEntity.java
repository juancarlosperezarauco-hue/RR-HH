package com.hrcoresystem.hrcore_api.common.security.authorization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_authorization_state")
public class TenantAuthorizationStateEntity {
    @Id
    private Short id;
    @Column(name = "owner_user_id")
    private UUID ownerUserId;
    @Column(nullable = false)
    private long authorizationVersion;
    @Column(nullable = false)
    private Instant updatedAt;

    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public long getAuthorizationVersion() { return authorizationVersion; }
    public void setAuthorizationVersion(long authorizationVersion) { this.authorizationVersion = authorizationVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
