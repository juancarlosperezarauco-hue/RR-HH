package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service.EmployeeFilePolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class EmployeeFilePolicyTest {
  private final EmployeeFileRepository files = mock(EmployeeFileRepository.class);
  private final EmployeeDocumentRepository documents = mock(EmployeeDocumentRepository.class);
  private final SecurityContextFacade security = mock(SecurityContextFacade.class);
  private final EmployeeFilePolicy policy =
      new EmployeeFilePolicy(
          files,
          documents,
          mock(TenantPlanEnforcementService.class),
          security,
          mock(AuditTrailService.class));

  @Test
  void confidentialDocumentRequiresExtraPermission() {
    UUID fileId = UUID.randomUUID(), documentId = UUID.randomUUID();
    var document = document(fileId, documentId, true);
    when(documents.findById(documentId)).thenReturn(Optional.of(document));

    assertThrows(
        AccessDeniedException.class,
        () -> policy.confidential(policy.document(fileId, documentId)));
  }

  @Test
  void documentCannotBeReadThroughAnotherFile() {
    UUID ownerFileId = UUID.randomUUID(), documentId = UUID.randomUUID();
    when(documents.findById(documentId))
        .thenReturn(Optional.of(document(ownerFileId, documentId, false)));

    assertThrows(
        ResourceNotFoundException.class, () -> policy.document(UUID.randomUUID(), documentId));
  }

  private EmployeeDocument document(UUID fileId, UUID id, boolean confidential) {
    return new EmployeeDocument(
        id,
        fileId,
        null,
        DocumentType.OTHER,
        "Document",
        null,
        null,
        null,
        null,
        confidential,
        VerificationStatus.PENDING,
        null,
        null,
        FileStatus.ACTIVE,
        null,
        null,
        null);
  }
}
