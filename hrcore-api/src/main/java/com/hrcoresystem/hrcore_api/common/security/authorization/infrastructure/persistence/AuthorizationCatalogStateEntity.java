package com.hrcoresystem.hrcore_api.common.security.authorization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "authorization_catalog_state", schema = "public")
public class AuthorizationCatalogStateEntity {
    @Id
    private Short id;
    @Column(nullable = false)
    private long catalogVersion;
    @Column(nullable = false)
    private Instant updatedAt;

    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }
    public long getCatalogVersion() { return catalogVersion; }
    public void setCatalogVersion(long catalogVersion) { this.catalogVersion = catalogVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
