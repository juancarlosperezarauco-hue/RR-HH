package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class EmployeeFilePolicy {
  private final EmployeeFileRepository files;
  private final EmployeeDocumentRepository documents;
  private final TenantPlanEnforcementService plans;
  private final SecurityContextFacade security;
  private final AuditTrailService audit;

  public EmployeeFilePolicy(
      EmployeeFileRepository files,
      EmployeeDocumentRepository documents,
      TenantPlanEnforcementService plans,
      SecurityContextFacade security,
      AuditTrailService audit) {
    this.files = files;
    this.documents = documents;
    this.plans = plans;
    this.security = security;
    this.audit = audit;
  }

  public void operational() {
    plans.assertCanOperate();
  }

  public EmployeeFile file(UUID id) {
    return files
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employee file not found"));
  }

  public EmployeeFile lockedFile(UUID id) {
    return files
        .findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employee file not found"));
  }

  public EmployeeDocument document(UUID fileId, UUID documentId) {
    EmployeeDocument d =
        documents
            .findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    if (!d.employeeFileId().equals(fileId))
      throw new ResourceNotFoundException("Document not found");
    return d;
  }

  public EmployeeDocument lockedDocument(UUID fileId, UUID documentId) {
    EmployeeDocument d =
        documents
            .findByIdForUpdate(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    if (!d.employeeFileId().equals(fileId))
      throw new ResourceNotFoundException("Document not found");
    return d;
  }

  public void confidential(EmployeeDocument d) {
    if (d.confidential() && !security.hasAuthority("employee_files.documents.confidential.read"))
      throw new AccessDeniedException("Confidential document permission required");
  }

  public boolean canReadConfidential() {
    return security.hasAuthority("employee_files.documents.confidential.read");
  }

  public void active(EmployeeFile f) {
    if (f.status() != FileStatus.ACTIVE) throw new BusinessException("Employee file is archived");
  }

  public void active(EmployeeDocument d) {
    if (d.status() != FileStatus.ACTIVE) throw new BusinessException("Document is archived");
  }

  public UUID actor() {
    try {
      return UUID.fromString(security.getCurrentSubject());
    } catch (RuntimeException ex) {
      throw new BusinessException("Authenticated tenant user is required");
    }
  }

  public String required(String s, int max, String field) {
    if (s == null || s.isBlank()) throw new BusinessException(field + " is required");
    String v = s.trim();
    if (v.length() > max) throw new BusinessException(field + " exceeds " + max + " characters");
    return v;
  }

  public String optional(String s, int max, String field) {
    if (s == null || s.isBlank()) return null;
    String v = s.trim();
    if (max > 0 && v.length() > max)
      throw new BusinessException(field + " exceeds " + max + " characters");
    return v;
  }

  public void audit(String action, String type, UUID id, Object details) {
    audit.recordTenantEvent(action, type, id.toString(), details);
  }
}
