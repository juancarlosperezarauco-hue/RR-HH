package com.hrcoresystem.hrcore_api.modules.tenant.shifts.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.CreateShiftAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.CreateShiftAssignmentsRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftAssignmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.UpdateShiftAssignmentRequest;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.service.EmployeeShiftAssignmentService;
import com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.ShiftAssignmentStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shifts/assignments")
@SecurityRequirement(name = "bearerAuth")
public class ShiftAssignmentController {
    private static final Set<String> SORT = Set.of("startDate", "endDate", "status", "createdAt", "id");
    private final EmployeeShiftAssignmentService assignments;

    public ShiftAssignmentController(EmployeeShiftAssignmentService assignments) {
        this.assignments = assignments;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('shifts.assignments.create')")
    public ApiResponse<ShiftAssignmentResponse> create(@Valid @RequestBody CreateShiftAssignmentRequest request) {
        return ApiResponse.success("Shift assignment created", assignments.create(request));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('shifts.assignments.create')")
    public ApiResponse<List<ShiftAssignmentResponse>> createBatch(
            @Valid @RequestBody CreateShiftAssignmentsRequest request) {
        return ApiResponse.success("Shift assignments created", assignments.createBatch(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('shifts.assignments.list')")
    public ApiResponse<Page<ShiftAssignmentResponse>> list(@RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID shiftId, @RequestParam(required = false) ShiftAssignmentStatus status,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Shift assignments retrieved",
                assignments.list(employeeId, shiftId, status, ShiftPageables.checked(pageable, SORT, "startDate")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('shifts.assignments.detail')")
    public ApiResponse<ShiftAssignmentResponse> detail(@PathVariable UUID id) {
        return ApiResponse.success("Shift assignment retrieved", assignments.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('shifts.assignments.update')")
    public ApiResponse<ShiftAssignmentResponse> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateShiftAssignmentRequest request) {
        return ApiResponse.success("Shift assignment updated", assignments.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('shifts.assignments.cancel')")
    public ApiResponse<ShiftAssignmentResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Shift assignment cancelled", assignments.cancel(id));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('shifts.assignments.close')")
    public ApiResponse<ShiftAssignmentResponse> close(@PathVariable UUID id) {
        return ApiResponse.success("Shift assignment closed", assignments.close(id));
    }
}
