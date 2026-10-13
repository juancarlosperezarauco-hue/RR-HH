package com.hrcoresystem.hrcore_api.modules.tenant.employees.application;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeePolicy;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeePolicyTest {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
    private final EmployeePolicy policy = new EmployeePolicy(repository, assignments,
            mock(TenantUserRepository.class), mock(TenantPlanEnforcementService.class), mock(SecurityContextFacade.class));

    @Test void createsActiveEmployeeAndNormalizesNames() {
        Employee employee = policy.normalized(null, request(" E-1 ", LocalDate.of(2024, 1, 1)), null, UUID.randomUUID());
        assertEquals("E-1", employee.employeeNumber());
        assertEquals(EmploymentStatus.ACTIVE, employee.employmentStatus());
        assertNull(employee.terminationDate());
    }
    @Test void rejectsDuplicateNumberAndDocument() {
        when(repository.existsNumberOtherThan("E-1", null)).thenReturn(true);
        assertThrows(BusinessException.class, () -> policy.normalized(null,
                request("E-1", LocalDate.of(2024, 1, 1)), null, UUID.randomUUID()));
        reset(repository);
        when(repository.existsDocumentOtherThan("CI", "123", null)).thenReturn(true);
        assertThrows(BusinessException.class, () -> policy.normalized(null,
                request("E-1", LocalDate.of(2024, 1, 1)), null, UUID.randomUUID()));
    }
    @Test void rejectsInvalidDatesAndTerminatedTransition() {
        assertThrows(BusinessException.class, () -> policy.normalized(null,
                request("E-1", LocalDate.of(1980, 1, 1)), null, UUID.randomUUID()));
        assertThrows(BusinessException.class, () -> policy.changedStatus(employee(EmploymentStatus.ACTIVE),
                EmploymentStatus.TERMINATED, UUID.randomUUID()));
        assertThrows(BusinessException.class, () -> policy.changedStatus(employee(EmploymentStatus.TERMINATED),
                EmploymentStatus.ACTIVE, UUID.randomUUID()));
    }
    @Test void inactiveStatusRequiresNoOpenOrFutureAssignment() {
        Employee e = employee(EmploymentStatus.ACTIVE);
        when(assignments.hasOpenOrFutureAssignments(e.id())).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> policy.changedStatus(e, EmploymentStatus.INACTIVE, UUID.randomUUID()));
    }

    private EmployeeRequest request(String number, LocalDate hireDate) {
        return new EmployeeRequest(null, number, "Ana", null, "Rojas", null, "CI", "123",
                LocalDate.of(1990, 1, 1), null, null, null, null, null, null, null, hireDate);
    }
    private Employee employee(EmploymentStatus status) {
        return new Employee(UUID.randomUUID(), null, "E-1", "Ana", null, "Rojas", null, "CI", "123",
                null, null, null, null, null, null, null, null, LocalDate.of(2020, 1, 1),
                null, status, null, null, null, null);
    }
}
