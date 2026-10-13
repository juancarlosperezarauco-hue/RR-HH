package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.EmployeeDocumentRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeDocumentRepositoryAdapter implements EmployeeDocumentRepository {
  private final EmployeeDocumentJpaRepository jpa;

  public EmployeeDocumentRepositoryAdapter(EmployeeDocumentJpaRepository jpa) {
    this.jpa = jpa;
  }

  public EmployeeDocument save(EmployeeDocument d) {
    var e = jpa.findById(d.id()).orElseGet(EmployeeDocumentEntity::new);
    e.setId(d.id());
    e.setEmployeeFileId(d.employeeFileId());
    e.setContractId(d.contractId());
    e.setDocumentType(d.documentType());
    e.setTitle(d.title());
    e.setDescription(d.description());
    e.setDocumentNumber(d.documentNumber());
    e.setIssuedAt(d.issuedAt());
    e.setExpiresAt(d.expiresAt());
    e.setConfidential(d.confidential());
    e.setVerificationStatus(d.verificationStatus());
    e.setVerifiedBy(d.verifiedBy());
    e.setVerifiedAt(d.verifiedAt());
    e.setStatus(d.status());
    e.setCreatedBy(d.createdBy());
    e.setCreatedAt(d.createdAt());
    return map(jpa.saveAndFlush(e));
  }

  public Optional<EmployeeDocument> findById(UUID id) {
    return jpa.findById(id).map(this::map);
  }

  public Optional<EmployeeDocument> findByIdForUpdate(UUID id) {
    return jpa.findByIdForUpdate(id).map(this::map);
  }

  public Page<EmployeeDocument> findAll(
      UUID fileId, EmployeeDocumentFilter filter, Pageable pageable) {
    Specification<EmployeeDocumentEntity> spec =
        (root, q, cb) -> {
          var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
          p.add(cb.equal(root.get("employeeFileId"), fileId));
          if (filter.type() != null) p.add(cb.equal(root.get("documentType"), filter.type()));
          if (filter.status() != null) p.add(cb.equal(root.get("status"), filter.status()));
          if (filter.expiresBefore() != null)
            p.add(cb.lessThanOrEqualTo(root.get("expiresAt"), filter.expiresBefore()));
          if (!filter.includeConfidential()) p.add(cb.isFalse(root.get("confidential")));
          return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    return jpa.findAll(spec, pageable).map(this::map);
  }

  private EmployeeDocument map(EmployeeDocumentEntity e) {
    return new EmployeeDocument(
        e.getId(),
        e.getEmployeeFileId(),
        e.getContractId(),
        e.getDocumentType(),
        e.getTitle(),
        e.getDescription(),
        e.getDocumentNumber(),
        e.getIssuedAt(),
        e.getExpiresAt(),
        e.isConfidential(),
        e.getVerificationStatus(),
        e.getVerifiedBy(),
        e.getVerifiedAt(),
        e.getStatus(),
        e.getCreatedBy(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
