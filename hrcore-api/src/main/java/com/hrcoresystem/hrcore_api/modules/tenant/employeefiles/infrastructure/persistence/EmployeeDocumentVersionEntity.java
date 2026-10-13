package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_document_versions")
@Getter
@Setter
public class EmployeeDocumentVersionEntity {
  @Id private UUID id;

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(name = "original_filename", nullable = false, length = 255)
  private String originalFilename;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(name = "mime_type", nullable = false, length = 150)
  private String mimeType;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "checksum_sha256", length = 64)
  private String checksumSha256;

  @Column(name = "uploaded_by")
  private UUID uploadedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void create() {
    if (createdAt == null) createdAt = Instant.now();
  }
}
