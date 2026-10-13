package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.FileStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_files")
@Getter
@Setter
public class EmployeeFileEntity {
  @Id private UUID id;

  @Column(name = "employee_id", nullable = false)
  private UUID employeeId;

  @Column(name = "file_number", nullable = false, length = 80)
  private String fileNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FileStatus status;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_by", updatable = false)
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void create() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void update() {
    updatedAt = Instant.now();
  }
}
