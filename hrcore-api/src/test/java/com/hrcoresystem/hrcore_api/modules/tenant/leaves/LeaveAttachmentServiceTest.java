package com.hrcoresystem.hrcore_api.modules.tenant.leaves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.*;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.DocumentStoragePort;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage.DocumentStorageProperties;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.LeaveAttachmentService;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.support.*;

class LeaveAttachmentServiceTest {
    private final UUID employeeId = UUID.randomUUID(), requestId = UUID.randomUUID();
    private final LeaveStore store = mock(LeaveStore.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final DocumentStoragePort storage = mock(DocumentStoragePort.class);
    private final DocumentStorageProperties properties = new DocumentStorageProperties();
    private final TenantPlanEnforcementService plans = mock(TenantPlanEnforcementService.class);
    private final SecurityContextFacade security = mock(SecurityContextFacade.class);
    private final AuditTrailService audit = mock(AuditTrailService.class);
    private final LeaveAttachmentService service = new LeaveAttachmentService(store, employees,
            storage, properties, plans, security, audit);

    @BeforeEach void setup() {
        TenantContextHolder.set(new TenantContext("acme", "tenant_acme", false));
        LeaveRequest draft = new LeaveRequest(requestId, employeeId, UUID.randomUUID(),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), null, null,
                BigDecimal.ONE, "Reason", RequestStatus.DRAFT, UUID.randomUUID(),
                null, null, null, null, null, null);
        when(store.request(requestId)).thenReturn(Optional.of(draft));
        when(store.lockedRequest(requestId)).thenReturn(Optional.of(draft));
        when(store.save(any(LeaveAttachment.class))).thenAnswer(i -> i.getArgument(0));
        when(employees.findByIdForUpdate(employeeId)).thenReturn(Optional.of(mock(Employee.class)));
        when(security.getCurrentSubject()).thenReturn(UUID.randomUUID().toString());
    }
    @AfterEach void cleanup() {
        TenantContextHolder.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.clearSynchronization();
    }

    @Test void rejectsFalsePdfBeforeStorageWrite() {
        var file = new MockMultipartFile("file", "bad.pdf", "application/pdf", new byte[] {'M', 'Z', 1});
        assertThrows(BusinessException.class, () -> service.upload(requestId, file));
        verify(storage, never()).put(anyString(), any(), anyString(), anyString());
    }

    @Test void rollbackDeletesUploadedObjectAndMetadataDoesNotExposeKey() {
        TransactionSynchronizationManager.initSynchronization();
        var file = new MockMultipartFile("file", "proof.pdf", "application/pdf", "%PDF-1.7\n".getBytes());
        var result = service.upload(requestId, file);
        assertEquals("proof.pdf", result.filename());
        assertTrue(Arrays.stream(result.getClass().getRecordComponents())
                .noneMatch(component -> component.getName().equals("storageKey")));
        verify(storage).put(contains("tenants/acme/employees/" + employeeId + "/leaves/" + requestId),
                any(), eq("application/pdf"), anyString());
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(storage).delete(contains("/attachments/" + result.id()));
    }

    @Test void refusesAttachmentWhoseStoredKeyPointsToAnotherTenant() {
        UUID attachmentId = UUID.randomUUID();
        when(store.attachment(attachmentId)).thenReturn(Optional.of(new LeaveAttachment(attachmentId,
                requestId, "proof.pdf", "tenants/other/leaves/proof", "application/pdf", 10, null, null)));
        assertThrows(ResourceNotFoundException.class, () -> service.prepareDownload(requestId, attachmentId));
        verify(storage, never()).get(anyString(), any(), anyLong());
    }

    @Test void failedObjectStorageDoesNotWriteAttachmentMetadata() {
        TransactionSynchronizationManager.initSynchronization();
        doThrow(new IllegalStateException("storage unavailable")).when(storage)
                .put(anyString(), any(), anyString(), anyString());
        var file = new MockMultipartFile("file", "proof.pdf", "application/pdf", "%PDF-1.7\n".getBytes());
        assertThrows(IllegalStateException.class, () -> service.upload(requestId, file));
        verify(store, never()).save(any(LeaveAttachment.class));
    }

    @Test void requiredAttachmentMustStillExistInPrivateStorage() throws Exception {
        String key = "tenants/acme/employees/" + employeeId + "/leaves/" + requestId + "/attachments/test";
        byte[] content = "%PDF-1.7\n".getBytes();
        LeaveAttachment attachment = new LeaveAttachment(UUID.randomUUID(), requestId,
                "proof.pdf", key, "application/pdf", content.length, null, null);
        when(store.hasAttachment(requestId)).thenReturn(true);
        when(store.attachments(eq(requestId), any())).thenReturn(new PageImpl<>(List.of(attachment)));
        doAnswer(i -> { Files.write(i.getArgument(1), content); return null; })
                .when(storage).get(eq(key), any(), anyLong());
        assertDoesNotThrow(() -> service.requireStoredAttachment(requestId));
        reset(storage);
        assertThrows(BusinessException.class, () -> service.requireStoredAttachment(requestId));
    }
}
