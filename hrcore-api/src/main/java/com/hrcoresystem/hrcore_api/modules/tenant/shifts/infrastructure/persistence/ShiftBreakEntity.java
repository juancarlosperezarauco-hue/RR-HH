package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shift_breaks")
@Getter
@Setter
public class ShiftBreakEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "shift_id", nullable = false, updatable = false)
    private UUID shiftId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private boolean paid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void beforeInsert() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
