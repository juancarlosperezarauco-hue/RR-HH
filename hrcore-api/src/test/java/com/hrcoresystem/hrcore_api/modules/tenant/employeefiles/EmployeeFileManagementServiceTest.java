package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

class EmployeeFileManagementServiceTest {
  private final EmployeeFileRepository files = mock(EmployeeFileRepository.class);
  private final EmployeeRepository employees = mock(EmployeeRepository.class);
  private final EmployeeFilePolicy policy = mock(EmployeeFilePolicy.class);
  private final EmployeeFileManagementService service =
      new EmployeeFileManagementService(files, employees, policy);

  @Test
  void duplicateNumberIsRejected() {
    UUID id = UUID.randomUUID();
    when(employees.findById(id))
        .thenReturn(
            Optional.of(
                mock(
                    com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.Employee
                        .class)));
    when(policy.required("HR-1", 80, "fileNumber")).thenReturn("HR-1");
    when(files.existsByFileNumberOtherThan("HR-1", null)).thenReturn(true);
    assertThrows(
        BusinessException.class, () -> service.create(new CreateFileRequest(id, "HR-1", null)));
    verify(files, never()).save(any());
  }

  @Test
  void unchangedUpdateDoesNotWriteOrAudit() {
    UUID id = UUID.randomUUID();
    var existing =
        new EmployeeFile(
            id,
            UUID.randomUUID(),
            "HR-1",
            FileStatus.ACTIVE,
            "note",
            UUID.randomUUID(),
            null,
            null);
    when(policy.lockedFile(id)).thenReturn(existing);
    when(policy.required("HR-1", 80, "fileNumber")).thenReturn("HR-1");
    service.update(id, new UpdateFileRequest("HR-1", "note", FileStatus.ACTIVE));
    verify(files, never()).save(any());
    verify(policy, never()).audit(anyString(), anyString(), any(), any());
  }

  @Test
  void archivedFileRejectsMetadataChanges() {
    UUID id = UUID.randomUUID();
    var archived =
        new EmployeeFile(
            id, UUID.randomUUID(), "HR-1", FileStatus.ARCHIVED, "note", null, null, null);
    when(policy.lockedFile(id)).thenReturn(archived);
    when(policy.required("HR-2", 80, "fileNumber")).thenReturn("HR-2");

    assertThrows(
        BusinessException.class,
        () -> service.update(id, new UpdateFileRequest("HR-2", "note", FileStatus.ACTIVE)));
    verify(files, never()).save(any());
  }

  @Test
  void listingDelegatesPaginationToRepository() {
    Pageable page = PageRequest.of(2, 5);
    when(files.findAll(any(), eq(page))).thenReturn(Page.empty(page));
    service.list(new EmployeeFileFilter(null, null, null), page);
    verify(files).findAll(any(), eq(page));
  }
}
