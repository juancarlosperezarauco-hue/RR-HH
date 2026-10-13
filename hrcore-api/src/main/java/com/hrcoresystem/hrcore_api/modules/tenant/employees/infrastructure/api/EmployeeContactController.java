package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees/{employeeId}/emergency-contacts")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeContactController {
    private static final Set<String> SORTABLE = Set.of("fullName", "primary", "createdAt");
    private final CreateEmployeeContactUseCase create; private final ListEmployeeContactsUseCase list;
    private final GetEmployeeContactUseCase get; private final UpdateEmployeeContactUseCase update;
    private final DeleteEmployeeContactUseCase delete;
    public EmployeeContactController(CreateEmployeeContactUseCase create, ListEmployeeContactsUseCase list,
                                     GetEmployeeContactUseCase get, UpdateEmployeeContactUseCase update,
                                     DeleteEmployeeContactUseCase delete) {
        this.create = create; this.list = list; this.get = get; this.update = update; this.delete = delete;
    }
    @PostMapping
    @PreAuthorize("hasAuthority('employees.contacts.create')")
    public ApiResponse<EmployeeEmergencyContactResponse> create(@PathVariable UUID employeeId,
            @Valid @RequestBody EmployeeEmergencyContactRequest request) {
        return ApiResponse.success("Emergency contact created successfully", create.execute(employeeId, request));
    }
    @GetMapping
    @PreAuthorize("hasAuthority('employees.contacts.list')")
    public ApiResponse<Page<EmployeeEmergencyContactResponse>> list(@PathVariable UUID employeeId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Emergency contacts retrieved successfully",
                list.execute(employeeId, EmployeePageables.checked(pageable, SORTABLE, "fullName")));
    }
    @GetMapping("/{contactId}")
    @PreAuthorize("hasAuthority('employees.contacts.detail')")
    public ApiResponse<EmployeeEmergencyContactResponse> get(@PathVariable UUID employeeId, @PathVariable UUID contactId) {
        return ApiResponse.success("Emergency contact retrieved successfully", get.execute(employeeId, contactId));
    }
    @PutMapping("/{contactId}")
    @PreAuthorize("hasAuthority('employees.contacts.update')")
    public ApiResponse<EmployeeEmergencyContactResponse> update(@PathVariable UUID employeeId, @PathVariable UUID contactId,
            @Valid @RequestBody EmployeeEmergencyContactRequest request) {
        return ApiResponse.success("Emergency contact updated successfully", update.execute(employeeId, contactId, request));
    }
    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAuthority('employees.contacts.delete')")
    public ApiResponse<Void> delete(@PathVariable UUID employeeId, @PathVariable UUID contactId) {
        delete.execute(employeeId, contactId);
        return ApiResponse.success("Emergency contact deleted successfully", null);
    }
}
