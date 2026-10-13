package com.hrcoresystem.hrcore_api.modules.tenant.positions.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.RequirementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "position_requirements")
@Getter @Setter
public class PositionRequirementEntity {
    @Id @GeneratedValue
    private UUID id;
    @Column(name = "position_id", nullable = false, updatable = false)
    private UUID positionId;
    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 30)
    private RequirementType requirementType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private boolean mandatory;
    @Column(name = "minimum_years_experience")
    private Integer minimumYearsExperience;
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
