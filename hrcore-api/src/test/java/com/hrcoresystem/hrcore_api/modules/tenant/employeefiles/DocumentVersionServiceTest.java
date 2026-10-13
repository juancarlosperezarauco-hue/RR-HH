package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.tenancy.context.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage.DocumentStorageProperties;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;

class DocumentVersionServiceTest {
  private final EmployeeDocumentRepository documents = mock(EmployeeDocumentRepository.class);
  private final EmployeeDocumentVersionRepository versions =
      mock(EmployeeDocumentVersionRepository.class);
  private final DocumentStoragePort storage = mock(DocumentStoragePort.class);
  private final EmployeeFilePolicy policy = mock(EmployeeFilePolicy.class);
  private final DocumentStorageProperties properties = new DocumentStorageProperties();
  private final DocumentVersionService service =
      new DocumentVersionService(documents, versions, storage, properties, policy);
  private final UUID fileId = UUID.randomUUID(),
      documentId = UUID.randomUUID(),
      employeeId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    TenantContextHolder.set(new TenantContext("acme", "tenant_acme", false));
    when(policy.file(fileId))
        .thenReturn(
            new EmployeeFile(fileId, employeeId, "F-1", FileStatus.ACTIVE, null, null, null, null));
    when(policy.lockedFile(fileId))
        .thenReturn(
            new EmployeeFile(fileId, employeeId, "F-1", FileStatus.ACTIVE, null, null, null, null));
    when(policy.lockedDocument(fileId, documentId)).thenReturn(document());
    when(policy.document(fileId, documentId)).thenReturn(document());
    when(policy.actor()).thenReturn(UUID.randomUUID());
  }

  @AfterEach
  void cleanup() {
    TenantContextHolder.clear();
    if (TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void rejectsExecutableMasqueradingAsPdf() {
    var file =
        new MockMultipartFile("file", "bad.pdf", "application/pdf", new byte[] {'M', 'Z', 1, 2});
    assertThrows(BusinessException.class, () -> service.upload(fileId, documentId, file));
    verify(storage, never()).put(anyString(), any(), anyString(), anyString());
  }

  @Test
  void rollbackDeletesOnlyNewObject() {
    TransactionSynchronizationManager.initSynchronization();
    when(versions.nextVersionNumber(documentId)).thenReturn(2);
    when(versions.save(any())).thenAnswer(i -> i.getArgument(0));
    var file = new MockMultipartFile("file", "ok.pdf", "application/pdf", "%PDF-1.7\n".getBytes());
    var result = service.upload(fileId, documentId, file);
    assertEquals(2, result.versionNumber());
    var synchronizations = TransactionSynchronizationManager.getSynchronizations();
    assertEquals(1, synchronizations.size());
    synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(storage).delete(contains("/versions/" + result.id()));
  }

  @Test
  void downloadChecksStoredChecksum() throws Exception {
    byte[] bytes = "%PDF-1.7\n".getBytes();
    var digest = MessageDigest.getInstance("SHA-256");
    String hash = HexFormat.of().formatHex(digest.digest(bytes));
    UUID versionId = UUID.randomUUID();
    var v =
        new EmployeeDocumentVersion(
            versionId,
            documentId,
            1,
            "ok.pdf",
            "tenants/acme/key",
            "application/pdf",
            bytes.length,
            hash,
            null,
            null);
    when(versions.findById(versionId)).thenReturn(Optional.of(v));
    doAnswer(
            i -> {
              Files.write(i.getArgument(1), bytes);
              return null;
            })
        .when(storage)
        .get(eq("tenants/acme/key"), any(), anyLong());
    var download = service.prepareDownload(fileId, documentId, versionId);
    try {
      assertArrayEquals(bytes, Files.readAllBytes(download.path()));
    } finally {
      download.cleanup();
    }
  }

  private EmployeeDocument document() {
    return new EmployeeDocument(
        documentId,
        fileId,
        null,
        DocumentType.OTHER,
        "Doc",
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
  }
}
