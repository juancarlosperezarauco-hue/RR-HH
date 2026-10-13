package com.hrcoresystem.hrcore_api.modules.tenant.departments.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.departments.domain.model.DepartmentStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {
    private static final Set<String> SORTABLE = Set.of("name", "code", "status", "createdAt", "updatedAt");
    private final CreateDepartmentUseCase create;
    private final ListDepartmentsUseCase list;
    private final GetDepartmentUseCase get;
    private final UpdateDepartmentUseCase update;
    private final ActivateDepartmentUseCase activate;
    private final DeactivateDepartmentUseCase deactivate;
    private final ListDepartmentHistoryUseCase history;

    public DepartmentController(CreateDepartmentUseCase create, ListDepartmentsUseCase list,
                                GetDepartmentUseCase get, UpdateDepartmentUseCase update,
                                ActivateDepartmentUseCase activate, DeactivateDepartmentUseCase deactivate,
                                ListDepartmentHistoryUseCase history) {
        this.create = create; this.list = list; this.get = get; this.update = update;
        this.activate = activate; this.deactivate = deactivate; this.history = history;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('departments.create')")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.success("Department created successfully", create.execute(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('departments.list')")
    public ApiResponse<Page<DepartmentResponse>> list(
            @RequestParam(required = false) DepartmentStatus status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) UUID parentDepartmentId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Departments retrieved successfully",
                list.execute(new DepartmentFilter(status, name, code, parentDepartmentId), checkedPage(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('departments.detail')")
    public ApiResponse<DepartmentResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Department retrieved successfully", get.execute(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('departments.update')")
    public ApiResponse<DepartmentResponse> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ApiResponse.success("Department updated successfully", update.execute(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('departments.activate')")
    public ApiResponse<DepartmentResponse> activate(@PathVariable UUID id,
            @RequestBody(required = false) DepartmentStatusRequest request) {
        return ApiResponse.success("Department activated successfully",
                activate.execute(id, request == null ? null : request.reason()));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('departments.deactivate')")
    public ApiResponse<DepartmentResponse> deactivate(@PathVariable UUID id,
            @RequestBody(required = false) DepartmentStatusRequest request) {
        return ApiResponse.success("Department deactivated successfully",
                deactivate.execute(id, request == null ? null : request.reason()));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('departments.history.read')")
    public ApiResponse<Page<DepartmentHistoryResponse>> history(@PathVariable UUID id,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        Pageable checked = checkedPage(pageable);
        Pageable chronological = PageRequest.of(checked.getPageNumber(), checked.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return ApiResponse.success("Department history retrieved successfully", history.execute(id, chronological));
    }

    private Pageable checkedPage(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100)
            throw new BusinessException("Page size must be between 1 and 100");
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) throw new BusinessException("Unsupported sort field: " + order.getProperty());
        }
        Sort sort = pageable.getSort().isUnsorted() ? Sort.by("name").ascending() : pageable.getSort();
        sort = sort.and(Sort.by("id").ascending());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
