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
@RequestMapping("/api/employees/{employeeId}/assignments")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeAssignmentController {
    private static final Set<String> SORTABLE = Set.of("startDate", "endDate", "assignmentType", "createdAt");
    private final CreateEmployeeAssignmentUseCase create; private final ListEmployeeAssignmentsUseCase list;
    private final GetEmployeeAssignmentUseCase get; private final UpdateEmployeeAssignmentUseCase update;
    private final CloseEmployeeAssignmentUseCase close;
    public EmployeeAssignmentController(CreateEmployeeAssignmentUseCase create, ListEmployeeAssignmentsUseCase list,
                                        GetEmployeeAssignmentUseCase get, UpdateEmployeeAssignmentUseCase update,
                                        CloseEmployeeAssignmentUseCase close) {
        this.create = create; this.list = list; this.get = get; this.update = update; this.close = close;
    }
    @PostMapping
    @PreAuthorize("hasAuthority('employees.assignments.create')")
    public ApiResponse<EmployeeAssignmentResponse> create(@PathVariable UUID employeeId,
            @Valid @RequestBody EmployeeAssignmentRequest request) {
        return ApiResponse.success("Assignment created successfully", create.execute(employeeId, request));
    }
    @GetMapping
    @PreAuthorize("hasAuthority('employees.assignments.list')")
    public ApiResponse<Page<EmployeeAssignmentResponse>> list(@PathVariable UUID employeeId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Assignments retrieved successfully",
                list.execute(employeeId, EmployeePageables.checked(pageable, SORTABLE, "startDate")));
    }
    @GetMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('employees.assignments.detail')")
    public ApiResponse<EmployeeAssignmentResponse> get(@PathVariable UUID employeeId, @PathVariable UUID assignmentId) {
        return ApiResponse.success("Assignment retrieved successfully", get.execute(employeeId, assignmentId));
    }
    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasAuthority('employees.assignments.update')")
    public ApiResponse<EmployeeAssignmentResponse> update(@PathVariable UUID employeeId,
            @PathVariable UUID assignmentId, @Valid @RequestBody EmployeeAssignmentRequest request) {
        return ApiResponse.success("Assignment updated successfully", update.execute(employeeId, assignmentId, request));
    }
    @PatchMapping("/{assignmentId}/close")
    @PreAuthorize("hasAuthority('employees.assignments.close')")
    public ApiResponse<EmployeeAssignmentResponse> close(@PathVariable UUID employeeId,
            @PathVariable UUID assignmentId, @Valid @RequestBody CloseAssignmentRequest request) {
        return ApiResponse.success("Assignment closed successfully", close.execute(employeeId, assignmentId, request.endDate()));
    }
}
