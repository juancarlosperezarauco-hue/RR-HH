package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;
import com.hrcoresystem.hrcore_api.modules.identity.access.domain.model.PermissionScope;

@Entity
@Table(name = "system_permissions", schema = "public")
public class SystemPermissionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionScope scope;

    @Column(nullable = false)
    private boolean customRoleAssignable;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }    public boolean isActive() {
        return active;
    }
    public PermissionScope getScope() { return scope; }
    public void setScope(PermissionScope scope) { this.scope = scope; }
    public boolean isCustomRoleAssignable() { return customRoleAssignable; }
    public void setCustomRoleAssignable(boolean value) { this.customRoleAssignable = value; }

    public void setActive(boolean active) {
        this.active = active;
    }    public Instant getCreatedAt() {
        return createdAt;
    }
}
