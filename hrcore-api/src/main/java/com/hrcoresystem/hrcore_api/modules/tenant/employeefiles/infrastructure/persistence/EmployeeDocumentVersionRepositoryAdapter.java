package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.EmployeeDocumentVersion;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.EmployeeDocumentVersionRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeDocumentVersionRepositoryAdapter implements EmployeeDocumentVersionRepository {
  private final EmployeeDocumentVersionJpaRepository jpa;

  public EmployeeDocumentVersionRepositoryAdapter(EmployeeDocumentVersionJpaRepository jpa) {
    this.jpa = jpa;
  }

  public EmployeeDocumentVersion save(EmployeeDocumentVersion v) {
    var e = new EmployeeDocumentVersionEntity();
    e.setId(v.id());
    e.setDocumentId(v.documentId());
    e.setVersionNumber(v.versionNumber());
    e.setOriginalFilename(v.originalFilename());
    e.setStorageKey(v.storageKey());
    e.setMimeType(v.mimeType());
    e.setFileSize(v.fileSize());
    e.setChecksumSha256(v.checksumSha256());
    e.setUploadedBy(v.uploadedBy());
    e.setCreatedAt(v.createdAt());
    return map(jpa.saveAndFlush(e));
  }

  public Optional<EmployeeDocumentVersion> findById(UUID id) {
    return jpa.findById(id).map(this::map);
  }

  public Page<EmployeeDocumentVersion> findAll(UUID documentId, Pageable pageable) {
    return jpa.findByDocumentId(documentId, pageable).map(this::map);
  }

  public int nextVersionNumber(UUID documentId) {
    return jpa.nextVersionNumber(documentId);
  }

  private EmployeeDocumentVersion map(EmployeeDocumentVersionEntity e) {
    return new EmployeeDocumentVersion(
        e.getId(),
        e.getDocumentId(),
        e.getVersionNumber(),
        e.getOriginalFilename(),
        e.getStorageKey(),
        e.getMimeType(),
        e.getFileSize(),
        e.getChecksumSha256(),
        e.getUploadedBy(),
        e.getCreatedAt());
  }
}
