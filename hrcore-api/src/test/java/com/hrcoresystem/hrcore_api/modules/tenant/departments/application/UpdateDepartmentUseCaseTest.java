package com.hrcoresystem.hrcore_api.modules.tenant.departments.application;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.mapper.DepartmentMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase.UpdateDepartmentUseCase;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateDepartmentUseCaseTest {
    private final DepartmentRepository repository = mock(DepartmentRepository.class);
    private final DepartmentPolicy policy = mock(DepartmentPolicy.class);
    private final DepartmentChangeRecorder recorder = mock(DepartmentChangeRecorder.class);
    private final DepartmentMapper mapper = new DepartmentMapper();
    private final UpdateDepartmentUseCase useCase = new UpdateDepartmentUseCase(repository, policy, recorder, mapper);

    @Test
    void noChangeDoesNotWriteHistoryOrAudit() {
        Department current = department(null);
        when(policy.require(current.id())).thenReturn(current);
        when(policy.required("D1", 50, "code")).thenReturn("D1");
        when(policy.required("Sales", 150, "name")).thenReturn("Sales");
        when(recorder.changed(eq(current), any(Department.class))).thenReturn(false);
        DepartmentResponse response = useCase.execute(current.id(),
                new UpdateDepartmentRequest("D1", "Sales", null, null, null, null, null));
        assertEquals(current.id(), response.id());
        verify(repository, never()).save(any());
        verify(recorder, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void parentChangeRequiresReason() {
        Department current = department(null);
        UUID newParent = UUID.randomUUID();
        when(policy.require(current.id())).thenReturn(current);
        when(policy.required("D1", 50, "code")).thenReturn("D1");
        when(policy.required("Sales", 150, "name")).thenReturn("Sales");
        when(policy.requiredReason(null)).thenThrow(new BusinessException("reason is required"));
        assertThrows(BusinessException.class, () -> useCase.execute(current.id(),
                new UpdateDepartmentRequest("D1", "Sales", null, newParent, null, null, null)));
        verify(repository, never()).save(any());
    }

    private Department department(UUID parent) {
        return new Department(UUID.randomUUID(), "D1", "Sales", null, parent, null, null,
                DepartmentStatus.ACTIVE, null, null, null, null);
    }
}
