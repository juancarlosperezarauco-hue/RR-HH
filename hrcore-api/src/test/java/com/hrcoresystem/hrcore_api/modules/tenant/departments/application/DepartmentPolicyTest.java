package com.hrcoresystem.hrcore_api.modules.tenant.departments.application;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.service.DepartmentPolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.Department;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentStatus;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepartmentPolicyTest {
    private final DepartmentRepository repository = mock(DepartmentRepository.class);
    private final DepartmentPolicy policy = new DepartmentPolicy(repository,
            mock(TenantPlanEnforcementService.class), mock(SecurityContextFacade.class));

    @Test
    void rejectsIndirectCycle() {
        UUID child = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        when(repository.findById(parent)).thenReturn(Optional.of(department(parent, child, DepartmentStatus.ACTIVE)));
        when(repository.findById(child)).thenReturn(Optional.of(department(child, null, DepartmentStatus.ACTIVE)));
        assertThrows(BusinessException.class, () -> policy.validateReferences(child, parent, null));
    }

    @Test
    void rejectsInactiveParentAndManager() {
        UUID parent = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        when(repository.findById(parent)).thenReturn(Optional.of(department(parent, null, DepartmentStatus.INACTIVE)));
        assertThrows(BusinessException.class, () -> policy.validateReferences(UUID.randomUUID(), parent, null));
        assertThrows(BusinessException.class, () -> policy.validateReferences(null, null, manager));
    }

    @Test
    void rejectsDeactivationWithActiveDependents() {
        UUID id = UUID.randomUUID();
        when(repository.hasActiveChildren(id)).thenReturn(true);
        assertThrows(BusinessException.class, () -> policy.validateDeactivation(id));
        when(repository.hasActiveChildren(id)).thenReturn(false);
        when(repository.hasCurrentAssignments(id)).thenReturn(true);
        assertThrows(BusinessException.class, () -> policy.validateDeactivation(id));
    }

    @Test
    void acceptsValidReferences() {
        UUID parent = UUID.randomUUID();
        UUID manager = UUID.randomUUID();
        when(repository.findById(parent)).thenReturn(Optional.of(department(parent, null, DepartmentStatus.ACTIVE)));
        when(repository.existsActiveEmployee(manager)).thenReturn(true);
        assertDoesNotThrow(() -> policy.validateReferences(UUID.randomUUID(), parent, manager));
    }

    private Department department(UUID id, UUID parent, DepartmentStatus status) {
        return new Department(id, "CODE", "Name", null, parent, null, null, status, null, null, null, null);
    }
}
