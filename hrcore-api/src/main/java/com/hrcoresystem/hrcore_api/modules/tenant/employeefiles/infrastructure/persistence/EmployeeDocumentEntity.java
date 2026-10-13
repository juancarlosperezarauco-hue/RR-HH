package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_documents")
@Getter
@Setter
public class EmployeeDocumentEntity {
  @Id private UUID id;

  @Column(name = "employee_file_id", nullable = false)
  private UUID employeeFileId;

  @Column(name = "contract_id")
  private UUID contractId;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false, length = 40)
  private DocumentType documentType;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "document_number", length = 100)
  private String documentNumber;

  @Column(name = "issued_at")
  private LocalDate issuedAt;

  @Column(name = "expires_at")
  private LocalDate expiresAt;

  @Column(nullable = false)
  private boolean confidential;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false, length = 20)
  private VerificationStatus verificationStatus;

  @Column(name = "verified_by")
  private UUID verifiedBy;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FileStatus status;

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
