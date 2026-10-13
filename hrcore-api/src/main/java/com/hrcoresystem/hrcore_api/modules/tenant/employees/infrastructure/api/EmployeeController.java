package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.*;
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
@RequestMapping("/api/employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {
    private static final Set<String> SORTABLE = Set.of("employeeNumber", "firstName", "lastName",
            "hireDate", "employmentStatus", "createdAt");
    private final CreateEmployeeUseCase create; private final ListEmployeesUseCase list;
    private final GetEmployeeUseCase get; private final UpdateEmployeeUseCase update;
    private final ChangeEmployeeStatusUseCase status;
    public EmployeeController(CreateEmployeeUseCase create, ListEmployeesUseCase list, GetEmployeeUseCase get,
                              UpdateEmployeeUseCase update, ChangeEmployeeStatusUseCase status) {
        this.create = create; this.list = list; this.get = get; this.update = update; this.status = status;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('employees.create')")
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success("Employee created successfully", create.execute(request));
    }
    @GetMapping
    @PreAuthorize("hasAuthority('employees.list')")
    public ApiResponse<Page<EmployeeSummaryResponse>> list(@RequestParam(required = false) String name,
            @RequestParam(required = false) String employeeNumber,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID positionId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Employees retrieved successfully", list.execute(
                new EmployeeFilter(name, employeeNumber, status, departmentId, positionId),
                EmployeePageables.checked(pageable, SORTABLE, "lastName")));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employees.detail')")
    public ApiResponse<EmployeeResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Employee retrieved successfully", get.execute(id));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('employees.update')")
    public ApiResponse<EmployeeResponse> update(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success("Employee updated successfully", update.execute(id, request));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('employees.status.update')")
    public ApiResponse<EmployeeResponse> status(@PathVariable UUID id, @Valid @RequestBody EmployeeStatusRequest request) {
        return ApiResponse.success("Employee status updated successfully", status.execute(id, request.status()));
    }
}
