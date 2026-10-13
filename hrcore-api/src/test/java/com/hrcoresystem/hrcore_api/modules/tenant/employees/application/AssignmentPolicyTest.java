package com.hrcoresystem.hrcore_api.modules.tenant.employees.application;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.repository.DepartmentRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.positions.domain.repository.PositionRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.EmployeeAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssignmentPolicyTest {
    private final EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final PositionRepository positions = mock(PositionRepository.class);
    private final EmployeePolicy employeePolicy = mock(EmployeePolicy.class);
    private final AssignmentPolicy policy = new AssignmentPolicy(assignments, employees, departments, positions, employeePolicy);
    private final UUID departmentId = UUID.randomUUID();
    private final UUID positionId = UUID.randomUUID();

    @Test void validatesReferencesAndSerializesAgainstDeactivation() {
        Employee e = employee();
        EmployeeAssignmentRequest request = request(LocalDate.now(), null);
        assertThrows(BusinessException.class, () -> policy.normalized(e, request, null, UUID.randomUUID()));
        when(departments.findById(departmentId)).thenReturn(Optional.of(department(DepartmentStatus.ACTIVE)));
        assertThrows(BusinessException.class, () -> policy.normalized(e, request, null, UUID.randomUUID()));
        when(positions.findByIdForUpdate(positionId)).thenReturn(Optional.of(position(PositionStatus.ACTIVE)));
        EmployeeAssignment result = policy.normalized(e, request, null, UUID.randomUUID());
        assertEquals(AssignmentType.PRIMARY, result.assignmentType());
        verify(departments, atLeastOnce()).lockHierarchy();
        verify(positions, atLeastOnce()).findByIdForUpdate(positionId);
    }
    @Test void rejectsOverlappingPrimaryAssignment() {
        Employee e = employee();
        when(departments.findById(departmentId)).thenReturn(Optional.of(department(DepartmentStatus.ACTIVE)));
        when(positions.findByIdForUpdate(positionId)).thenReturn(Optional.of(position(PositionStatus.ACTIVE)));
        when(assignments.hasOverlappingPrimary(eq(e.id()), isNull(), any(), isNull())).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> policy.normalized(e, request(LocalDate.now(), null), null, UUID.randomUUID()));
    }
    @Test void startedAndClosedAssignmentsCannotBeEdited() {
        Employee e = employee();
        EmployeeAssignment started = assignment(e.id(), LocalDate.now(), null);
        assertThrows(BusinessException.class,
                () -> policy.normalized(e, request(LocalDate.now(), null), started, UUID.randomUUID()));
        EmployeeAssignment closed = assignment(e.id(), LocalDate.now().minusDays(4), LocalDate.now().minusDays(1));
        assertThrows(BusinessException.class,
                () -> policy.normalized(e, request(LocalDate.now().plusDays(2), null), closed, UUID.randomUUID()));
    }
    @Test void closeRequiresStartedOpenAssignmentAndValidDate() {
        EmployeeAssignment future = assignment(UUID.randomUUID(), LocalDate.now().plusDays(1), null);
        assertThrows(BusinessException.class, () -> policy.closed(future, LocalDate.now()));
        EmployeeAssignment started = assignment(UUID.randomUUID(), LocalDate.now().minusDays(2), null);
        assertEquals(LocalDate.now(), policy.closed(started, LocalDate.now()).endDate());
        assertThrows(BusinessException.class, () -> policy.closed(started, LocalDate.now().plusDays(1)));
    }
    @Test void assignmentMustBelongToRequestedEmployee() {
        UUID employeeId = UUID.randomUUID(), assignmentId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> policy.require(employeeId, assignmentId));
        verify(assignments).findByIdAndEmployeeId(assignmentId, employeeId);
    }

    private EmployeeAssignmentRequest request(LocalDate start, LocalDate end) {
        return new EmployeeAssignmentRequest(departmentId, positionId, null, start, end, null, null);
    }
    private Employee employee() {
        return new Employee(UUID.randomUUID(), null, "E-1", "Ana", null, "Rojas", null, "CI", "123",
                null, null, null, null, null, null, null, null, LocalDate.of(2020, 1, 1), null,
                EmploymentStatus.ACTIVE, null, null, null, null);
    }
    private EmployeeAssignment assignment(UUID employeeId, LocalDate start, LocalDate end) {
        return new EmployeeAssignment(UUID.randomUUID(), employeeId, departmentId, positionId,
                null, start, end, AssignmentType.PRIMARY, null, null, null, null);
    }
    private Department department(DepartmentStatus status) {
        return new Department(departmentId, "D1", "Dept", null, null, null, null, status, null, null, null, null);
    }
    private Position position(PositionStatus status) {
        return new Position(positionId, "P1", "Position", null, null, OrganizationalLevel.OPERATIONAL,
                null, null, null, status, null, null, null, null);
    }
}
