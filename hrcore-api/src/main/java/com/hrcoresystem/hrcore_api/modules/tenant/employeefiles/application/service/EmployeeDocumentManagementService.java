package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.EmploymentContractRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeDocumentManagementService {
  private final EmployeeDocumentRepository documents;
  private final EmployeeDocumentVersionRepository versions;
  private final EmploymentContractRepository contracts;
  private final EmployeeFilePolicy policy;

  public EmployeeDocumentManagementService(
      EmployeeDocumentRepository documents,
      EmployeeDocumentVersionRepository versions,
      EmploymentContractRepository contracts,
      EmployeeFilePolicy policy) {
    this.documents = documents;
    this.versions = versions;
    this.contracts = contracts;
    this.policy = policy;
  }

  @Transactional
  public DocumentResponse create(UUID fileId, CreateDocumentRequest r) {
    policy.operational();
    var file = policy.lockedFile(fileId);
    policy.active(file);
    validate(r.issuedAt(), r.expiresAt(), r.contractId(), file.employeeId());
    var saved =
        documents.save(
            new EmployeeDocument(
                UUID.randomUUID(),
                fileId,
                r.contractId(),
                r.documentType(),
                policy.required(r.title(), 200, "title"),
                policy.optional(r.description(), 0, "description"),
                policy.optional(r.documentNumber(), 100, "documentNumber"),
                r.issuedAt(),
                r.expiresAt(),
                r.confidential(),
                VerificationStatus.PENDING,
                null,
                null,
                FileStatus.ACTIVE,
                policy.actor(),
                null,
                null));
    policy.audit(
        "EMPLOYEE_DOCUMENT_CREATED",
        "EMPLOYEE_DOCUMENT",
        saved.id(),
        Map.of("fileId", fileId, "confidential", saved.confidential()));
    return map(saved);
  }

  @Transactional
  public Page<DocumentResponse> list(
      UUID fileId, EmployeeDocumentFilter filter, Pageable pageable) {
    policy.operational();
    policy.file(fileId);
    boolean allowed = policy.canReadConfidential();
    var effective =
        new EmployeeDocumentFilter(filter.type(), filter.status(), filter.expiresBefore(), allowed);
    Page<DocumentResponse> page = documents.findAll(fileId, effective, pageable).map(this::map);
    if (allowed && page.getContent().stream().anyMatch(DocumentResponse::confidential))
      policy.audit(
          "CONFIDENTIAL_DOCUMENTS_LISTED",
          "EMPLOYEE_FILE",
          fileId,
          Map.of("count", page.getNumberOfElements()));
    return page;
  }

  @Transactional
  public DocumentResponse get(UUID fileId, UUID documentId) {
    policy.operational();
    policy.file(fileId);
    var d = policy.document(fileId, documentId);
    policy.confidential(d);
    if (d.confidential())
      policy.audit("CONFIDENTIAL_DOCUMENT_READ", "EMPLOYEE_DOCUMENT", documentId, Map.of());
    return map(d);
  }

  @Transactional
  public DocumentResponse update(UUID fileId, UUID documentId, UpdateDocumentRequest r) {
    policy.operational();
    var file = policy.lockedFile(fileId);
    policy.active(file);
    var old = policy.lockedDocument(fileId, documentId);
    policy.confidential(old);
    validate(r.issuedAt(), r.expiresAt(), r.contractId(), file.employeeId());
    String title = policy.required(r.title(), 200, "title"),
        description = policy.optional(r.description(), 0, "description"),
        number = policy.optional(r.documentNumber(), 100, "documentNumber");
    boolean metadataChanged =
        !Objects.equals(old.contractId(), r.contractId())
            || old.documentType() != r.documentType()
            || !old.title().equals(title)
            || !Objects.equals(old.description(), description)
            || !Objects.equals(old.documentNumber(), number)
            || !Objects.equals(old.issuedAt(), r.issuedAt())
            || !Objects.equals(old.expiresAt(), r.expiresAt())
            || old.confidential() != r.confidential();
    if (old.status() == FileStatus.ARCHIVED && metadataChanged)
      throw new BusinessException("Archived document can only be reactivated");
    if (!metadataChanged && old.status() == r.status()) return map(old);
    var saved =
        documents.save(
            new EmployeeDocument(
                old.id(),
                fileId,
                r.contractId(),
                r.documentType(),
                title,
                description,
                number,
                r.issuedAt(),
                r.expiresAt(),
                r.confidential(),
                metadataChanged ? VerificationStatus.PENDING : old.verificationStatus(),
                metadataChanged ? null : old.verifiedBy(),
                metadataChanged ? null : old.verifiedAt(),
                r.status(),
                old.createdBy(),
                old.createdAt(),
                old.updatedAt()));
    policy.audit(
        "EMPLOYEE_DOCUMENT_UPDATED",
        "EMPLOYEE_DOCUMENT",
        old.id(),
        Map.of("metadataChanged", metadataChanged, "statusChanged", old.status() != r.status()));
    return map(saved);
  }

  @Transactional
  public DocumentResponse verify(UUID fileId, UUID documentId, VerificationStatus target) {
    policy.operational();
    var file = policy.lockedFile(fileId);
    policy.active(file);
    var old = policy.lockedDocument(fileId, documentId);
    policy.confidential(old);
    policy.active(old);
    if (target == VerificationStatus.PENDING)
      throw new BusinessException("Verification decision must be VERIFIED or REJECTED");
    if (versions.nextVersionNumber(documentId) == 1)
      throw new BusinessException("Document has no version");
    if (old.verificationStatus() == target) return map(old);
    var saved =
        documents.save(
            new EmployeeDocument(
                old.id(),
                fileId,
                old.contractId(),
                old.documentType(),
                old.title(),
                old.description(),
                old.documentNumber(),
                old.issuedAt(),
                old.expiresAt(),
                old.confidential(),
                target,
                policy.actor(),
                Instant.now(),
                old.status(),
                old.createdBy(),
                old.createdAt(),
                old.updatedAt()));
    policy.audit(
        "EMPLOYEE_DOCUMENT_VERIFIED",
        "EMPLOYEE_DOCUMENT",
        old.id(),
        Map.of("decision", target.name()));
    return map(saved);
  }

  private void validate(LocalDate issued, LocalDate expires, UUID contractId, UUID employeeId) {
    if (issued != null && expires != null && expires.isBefore(issued))
      throw new BusinessException("expiresAt must not precede issuedAt");
    if (contractId != null) {
      var contract =
          contracts
              .findById(contractId)
              .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
      if (!contract.employeeId().equals(employeeId))
        throw new BusinessException("Contract belongs to another employee");
    }
  }

  private DocumentResponse map(EmployeeDocument d) {
    return new DocumentResponse(
        d.id(),
        d.employeeFileId(),
        d.contractId(),
        d.documentType(),
        d.title(),
        d.description(),
        d.documentNumber(),
        d.issuedAt(),
        d.expiresAt(),
        d.confidential(),
        d.verificationStatus(),
        d.verifiedBy(),
        d.verifiedAt(),
        d.status(),
        d.createdAt(),
        d.updatedAt());
  }
}
