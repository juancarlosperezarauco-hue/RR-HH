package com.hrcoresystem.hrcore_api.modules.tenant.employees.application;

import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.mapper.EmployeeMapper;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmployeeUseCasesTest {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final EmployeeAssignmentRepository assignments = mock(EmployeeAssignmentRepository.class);
    private final EmployeeEmergencyContactRepository contacts = mock(EmployeeEmergencyContactRepository.class);
    private final EmployeePolicy employees = mock(EmployeePolicy.class);
    private final AssignmentPolicy assignmentPolicy = mock(AssignmentPolicy.class);
    private final ContactPolicy contactPolicy = mock(ContactPolicy.class);
    private final EmployeeAudit audit = mock(EmployeeAudit.class);
    private final EmployeeMapper mapper = new EmployeeMapper();

    @Test void createAuditsAndListReturnsOnlySummary() {
        Employee e = employee(EmploymentStatus.ACTIVE);
        EmployeeRequest request = request();
        when(employees.actorId()).thenReturn(UUID.randomUUID());
        when(employees.normalized(eq(null), eq(request), eq(null), any())).thenReturn(e);
        when(repository.save(e)).thenReturn(e);
        assertEquals(e.id(), new CreateEmployeeUseCase(repository, employees, audit, mapper).execute(request).id());
        verify(audit).record("EMPLOYEE", "CREATED", e.id(), List.of());
        var page = PageRequest.of(1, 5);
        var filter = new EmployeeFilter("Ana", null, null, null, null);
        when(repository.findAll(filter, page)).thenReturn(new PageImpl<>(List.of(e), page, 6));
        var listed = new ListEmployeesUseCase(repository, employees, mapper).execute(filter, page);
        assertEquals(6, listed.getTotalElements());
        assertEquals(e.employeeNumber(), listed.getContent().getFirst().employeeNumber());
    }
    @Test void unchangedUpdateAndStatusDoNotWriteOrAudit() {
        Employee e = employee(EmploymentStatus.ACTIVE);
        when(employees.requireForUpdate(e.id())).thenReturn(e);
        when(employees.actorId()).thenReturn(UUID.randomUUID());
        when(employees.normalized(eq(e.id()), eq(request()), eq(e), any())).thenReturn(e);
        when(employees.changedFields(e, e)).thenReturn(List.of());
        new UpdateEmployeeUseCase(repository, employees, audit, mapper).execute(e.id(), request());
        new ChangeEmployeeStatusUseCase(repository, employees, audit, mapper).execute(e.id(), EmploymentStatus.ACTIVE);
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);
    }
    @Test void assignmentCreateAndContactDeleteAuditWithoutPrivateValues() {
        Employee e = employee(EmploymentStatus.ACTIVE);
        when(employees.requireForUpdate(e.id())).thenReturn(e);
        when(employees.actorId()).thenReturn(UUID.randomUUID());
        EmployeeAssignmentRequest request = new EmployeeAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(),
                null, LocalDate.now(), null, AssignmentType.PRIMARY, null);
        EmployeeAssignment assignment = new EmployeeAssignment(UUID.randomUUID(), e.id(), request.departmentId(),
                request.positionId(), null, request.startDate(), null, AssignmentType.PRIMARY, null, null, null, null);
        when(assignmentPolicy.normalized(eq(e), eq(request), eq(null), any())).thenReturn(assignment);
        when(assignments.save(assignment)).thenReturn(assignment);
        new CreateEmployeeAssignmentUseCase(assignments, employees, assignmentPolicy, audit, mapper).execute(e.id(), request);
        verify(audit).record("EMPLOYEE_ASSIGNMENT", "CREATED", assignment.id(), List.of());

        UUID contactId = UUID.randomUUID();
        EmployeeEmergencyContact contact = new EmployeeEmergencyContact(contactId, e.id(), "Private Name", null,
                "+59170000000", null, null, false, null, null);
        when(contactPolicy.require(e.id(), contactId)).thenReturn(contact);
        new DeleteEmployeeContactUseCase(contacts, employees, contactPolicy, audit).execute(e.id(), contactId);
        verify(contacts).delete(contact);
        verify(audit).record("EMPLOYEE_CONTACT", "DELETED", contactId, List.of());
    }

    @Test void onboardingCreatesEmployeeAndItsInitialAssignmentAsOneUseCase() {
        CreateEmployeeUseCase createEmployee = mock(CreateEmployeeUseCase.class);
        CreateEmployeeAssignmentUseCase createAssignment = mock(CreateEmployeeAssignmentUseCase.class);
        EmployeeRequest employeeRequest = request();
        EmployeeAssignmentRequest assignmentRequest = new EmployeeAssignmentRequest(
                UUID.randomUUID(), UUID.randomUUID(), null, employeeRequest.hireDate(), null,
                AssignmentType.PRIMARY, "Initial assignment");
        UUID employeeId = UUID.randomUUID();
        EmployeeResponse employeeResponse = new EmployeeResponse(employeeId, null, "E-1", "Ana", null,
                "Rojas", null, "CI", "123", null, null, null, null, null, null, null,
                null, employeeRequest.hireDate(), null, EmploymentStatus.ACTIVE, null, null);
        EmployeeAssignmentResponse assignmentResponse = new EmployeeAssignmentResponse(UUID.randomUUID(),
                employeeId, assignmentRequest.departmentId(), assignmentRequest.positionId(), null,
                assignmentRequest.startDate(), null, AssignmentType.PRIMARY, "Initial assignment", null, null);
        when(createEmployee.execute(employeeRequest)).thenReturn(employeeResponse);
        when(createAssignment.execute(employeeId, assignmentRequest)).thenReturn(assignmentResponse);

        var result = new CreateEmployeeWithAssignmentUseCase(createEmployee, createAssignment)
                .execute(new EmployeeOnboardingRequest(employeeRequest, assignmentRequest));

        assertEquals(employeeId, result.employee().id());
        assertEquals(employeeId, result.assignment().employeeId());
        verify(createEmployee).execute(employeeRequest);
        verify(createAssignment).execute(employeeId, assignmentRequest);
    }

    private EmployeeRequest request() {
        return new EmployeeRequest(null, "E-1", "Ana", null, "Rojas", null, "CI", "123",
                null, null, null, null, null, null, null, null, LocalDate.of(2020, 1, 1));
    }
    private Employee employee(EmploymentStatus status) {
        return new Employee(UUID.randomUUID(), null, "E-1", "Ana", null, "Rojas", null, "CI", "123",
                null, null, null, null, null, null, null, null, LocalDate.of(2020, 1, 1),
                null, status, null, null, null, null);
    }
}
