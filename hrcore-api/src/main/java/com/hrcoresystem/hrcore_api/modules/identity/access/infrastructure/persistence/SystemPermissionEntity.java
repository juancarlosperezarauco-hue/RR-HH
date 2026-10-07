package com.hrcoresystem.hrcore_api.modules.identity.access.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_permissions")
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

    public void setActive(boolean active) {
        this.active = active;
    }    public Instant getCreatedAt() {
        return createdAt;
    }
}