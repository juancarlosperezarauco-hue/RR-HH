package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "positions")
@Getter @Setter
public class PositionEntity {
    @Id @GeneratedValue
    private UUID id;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String responsibilities;
    @Enumerated(EnumType.STRING)
    @Column(name = "organizational_level", nullable = false, length = 30)
    private OrganizationalLevel organizationalLevel;
    @Column(name = "salary_min", precision = 19, scale = 4)
    private BigDecimal salaryMin;
    @Column(name = "salary_max", precision = 19, scale = 4)
    private BigDecimal salaryMax;
    @Column(length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PositionStatus status;
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
    @Column(name = "updated_by")
    private UUID updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
