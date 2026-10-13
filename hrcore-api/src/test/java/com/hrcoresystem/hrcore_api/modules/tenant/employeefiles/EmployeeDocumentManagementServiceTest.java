package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.EmploymentContract;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.repository.EmploymentContractRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.CreateDocumentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

class EmployeeDocumentManagementServiceTest {
  @Test
  void confidentialFilterIsAppliedBeforePagination() {
    var repo = mock(EmployeeDocumentRepository.class);
    var versions = mock(EmployeeDocumentVersionRepository.class);
    var contracts = mock(EmploymentContractRepository.class);
    var policy = mock(EmployeeFilePolicy.class);
    var service = new EmployeeDocumentManagementService(repo, versions, contracts, policy);
    UUID fileId = UUID.randomUUID();
    Pageable page = PageRequest.of(1, 10);
    when(repo.findAll(eq(fileId), any(), eq(page))).thenReturn(Page.empty(page));
    service.list(fileId, new EmployeeDocumentFilter(null, null, null, true), page);
    var filter = org.mockito.ArgumentCaptor.forClass(EmployeeDocumentFilter.class);
    verify(repo).findAll(eq(fileId), filter.capture(), eq(page));
    assertFalse(filter.getValue().includeConfidential());
  }

  @Test
  void verificationRequiresUploadedVersion() {
    var repo = mock(EmployeeDocumentRepository.class);
    var versions = mock(EmployeeDocumentVersionRepository.class);
    var policy = mock(EmployeeFilePolicy.class);
    var service =
        new EmployeeDocumentManagementService(
            repo, versions, mock(EmploymentContractRepository.class), policy);
    UUID fileId = UUID.randomUUID(), id = UUID.randomUUID();
    var d =
        new EmployeeDocument(
            id,
            fileId,
            null,
            DocumentType.OTHER,
            "Certificate",
            null,
            null,
            null,
            null,
            false,
            VerificationStatus.PENDING,
            null,
            null,
            FileStatus.ACTIVE,
            null,
            null,
            null);
    when(policy.lockedDocument(fileId, id)).thenReturn(d);
    when(versions.nextVersionNumber(id)).thenReturn(1);
    assertThrows(
        com.hrcoresystem.hrcore_api.common.exception.BusinessException.class,
        () -> service.verify(fileId, id, VerificationStatus.VERIFIED));
    verify(repo, never()).save(any());
  }

  @Test
  void unchangedDocumentUpdateDoesNotWriteOrAudit() {
    var repo = mock(EmployeeDocumentRepository.class);
    var policy = mock(EmployeeFilePolicy.class);
    var service =
        new EmployeeDocumentManagementService(
            repo,
            mock(EmployeeDocumentVersionRepository.class),
            mock(EmploymentContractRepository.class),
            policy);
    UUID fileId = UUID.randomUUID(), id = UUID.randomUUID();
    var old =
        new EmployeeDocument(
            id,
            fileId,
            null,
            DocumentType.OTHER,
            "Certificate",
            null,
            null,
            null,
            null,
            false,
            VerificationStatus.VERIFIED,
            null,
            null,
            FileStatus.ACTIVE,
            null,
            null,
            null);
    when(policy.lockedFile(fileId))
        .thenReturn(
            new EmployeeFile(
                fileId, UUID.randomUUID(), "F-1", FileStatus.ACTIVE, null, null, null, null));
    when(policy.lockedDocument(fileId, id)).thenReturn(old);
    when(policy.required("Certificate", 200, "title")).thenReturn("Certificate");
    var request =
        new com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto
            .UpdateDocumentRequest(
            null,
            DocumentType.OTHER,
            "Certificate",
            null,
            null,
            null,
            null,
            false,
            FileStatus.ACTIVE);
    service.update(fileId, id, request);
    verify(repo, never()).save(any());
    verify(policy, never()).audit(anyString(), anyString(), any(), any());
  }

  @Test
  void contractMustBelongToEmployeeInFile() {
    var repo = mock(EmployeeDocumentRepository.class);
    var contracts = mock(EmploymentContractRepository.class);
    var policy = mock(EmployeeFilePolicy.class);
    var service =
        new EmployeeDocumentManagementService(
            repo, mock(EmployeeDocumentVersionRepository.class), contracts, policy);
    UUID fileId = UUID.randomUUID(), employeeId = UUID.randomUUID(), contractId = UUID.randomUUID();
    when(policy.lockedFile(fileId))
        .thenReturn(
            new EmployeeFile(fileId, employeeId, "F-1", FileStatus.ACTIVE, null, null, null, null));
    var contract = mock(EmploymentContract.class);
    when(contract.employeeId()).thenReturn(UUID.randomUUID());
    when(contracts.findById(contractId)).thenReturn(Optional.of(contract));

    assertThrows(
        BusinessException.class,
        () ->
            service.create(
                fileId,
                new CreateDocumentRequest(
                    contractId, DocumentType.CONTRACT, "Contract", null, null, null, null, false)));
    verify(repo, never()).save(any());
  }
}
