package com.hrcoresystem.hrcore_api.modules.tenant.contracts.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service.ContractManagementService;
import com.hrcoresystem.hrcore_api.modules.tenant.contracts.domain.model.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/contracts")
@SecurityRequirement(name = "bearerAuth")
public class ContractController {
    private static final Set<String> SORTABLE = Set.of("contractNumber", "contractType", "status",
            "startDate", "endDate", "baseSalary", "createdAt", "updatedAt");
    private final ContractManagementService service;
    private final com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service.ContractPdfService pdf;
    public ContractController(ContractManagementService service,
                              com.hrcoresystem.hrcore_api.modules.tenant.contracts.application.service.ContractPdfService pdf) {
        this.service = service; this.pdf = pdf;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contracts.create')")
    public ApiResponse<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return ApiResponse.success("Contract created successfully", service.create(request));
    }
    @GetMapping
    @PreAuthorize("hasAuthority('contracts.list')")
    public ApiResponse<Page<ContractResponse>> list(@RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) ContractType type,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) LocalDate expiringBefore,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 25) Pageable pageable) {
        return ApiResponse.success("Contracts retrieved successfully",
                service.list(new ContractFilter(employeeId, type, status, expiringBefore, search), checked(pageable)));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('contracts.detail')")
    public ApiResponse<ContractResponse> get(@PathVariable UUID id) {
        return ApiResponse.success("Contract retrieved successfully", service.get(id));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('contracts.update')")
    public ApiResponse<ContractResponse> update(@PathVariable UUID id, @Valid @RequestBody ContractRequest request) {
        return ApiResponse.success("Contract updated successfully", service.update(id, request));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('contracts.status.update')")
    public ApiResponse<ContractResponse> changeStatus(@PathVariable UUID id,
            @Valid @RequestBody ContractStatusRequest request) {
        return ApiResponse.success("Contract status updated successfully", service.changeStatus(id, request));
    }
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('contracts.renew')")
    public ApiResponse<ContractResponse> renew(@PathVariable UUID id,
            @Valid @RequestBody ContractRenewalRequest request) {
        return ApiResponse.success("Contract renewal created successfully", service.renew(id, request));
    }
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('contracts.history.read')")
    public ApiResponse<List<ContractHistoryResponse>> history(@PathVariable UUID id) {
        return ApiResponse.success("Contract history retrieved successfully", service.history(id));
    }
    @GetMapping(value = "/{id}/document", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('contracts.document.download')")
    public ResponseEntity<byte[]> document(@PathVariable UUID id) {
        var result = pdf.generate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .body(result.content());
    }
    private Pageable checked(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 100)
            throw new BusinessException("Page size must be between 1 and 100");
        for (Sort.Order order : pageable.getSort())
            if (!SORTABLE.contains(order.getProperty())) throw new BusinessException("Unsupported sort field: " + order.getProperty());
        Sort sort = pageable.getSort().isUnsorted() ? Sort.by("createdAt").descending() : pageable.getSort();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by("id").ascending()));
    }
}
