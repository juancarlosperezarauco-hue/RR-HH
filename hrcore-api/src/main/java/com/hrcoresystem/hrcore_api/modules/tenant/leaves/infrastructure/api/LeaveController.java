package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.dto.LeaveDtos.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.LeaveAttachmentService;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.LeaveService;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/leaves")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave management")
public class LeaveController {
    private final LeaveService service;
    private final LeaveAttachmentService files;

    public LeaveController(LeaveService service, LeaveAttachmentService files) {
        this.service = service; this.files = files;
    }

    @PostMapping("/types") @PreAuthorize("hasAuthority('leaves.types.create')")
    public ApiResponse<TypeResponse> createType(@Valid @RequestBody TypeInput input) {
        return ApiResponse.success("Leave type created", service.createType(input));
    }
    @GetMapping("/types") @PreAuthorize("hasAuthority('leaves.types.list')")
    public ApiResponse<Page<TypeResponse>> types(@RequestParam(required = false) String code,
            @RequestParam(required = false) String name, @RequestParam(required = false) TypeStatus status,
            @ParameterObject @PageableDefault(size = 25) Pageable page) {
        return ApiResponse.success("Leave types retrieved", service.types(code, name, status,
                checked(page, Set.of("code", "name", "status", "createdAt"), "code")));
    }
    @GetMapping("/types/{id}") @PreAuthorize("hasAuthority('leaves.types.detail')")
    public ApiResponse<TypeResponse> type(@PathVariable UUID id) {
        return ApiResponse.success("Leave type retrieved", service.type(id));
    }
    @PutMapping("/types/{id}") @PreAuthorize("hasAuthority('leaves.types.update')")
    public ApiResponse<TypeResponse> updateType(@PathVariable UUID id, @Valid @RequestBody TypeInput input) {
        return ApiResponse.success("Leave type updated", service.updateType(id, input));
    }
    @PatchMapping("/types/{id}/activate") @PreAuthorize("hasAuthority('leaves.types.activate')")
    public ApiResponse<TypeResponse> activate(@PathVariable UUID id) {
        return ApiResponse.success("Leave type activated", service.typeStatus(id, TypeStatus.ACTIVE));
    }
    @PatchMapping("/types/{id}/deactivate") @PreAuthorize("hasAuthority('leaves.types.deactivate')")
    public ApiResponse<TypeResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.success("Leave type deactivated", service.typeStatus(id, TypeStatus.INACTIVE));
    }

    @PostMapping("/balances") @PreAuthorize("hasAuthority('leaves.balances.create')")
    public ApiResponse<BalanceResponse> createBalance(@Valid @RequestBody BalanceInput input) {
        return ApiResponse.success("Leave balance created", service.createBalance(input));
    }
    @GetMapping("/balances") @PreAuthorize("hasAuthority('leaves.balances.list')")
    public ApiResponse<Page<BalanceResponse>> balances(@RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID leaveTypeId, @RequestParam(required = false) Integer year,
            @ParameterObject @PageableDefault(size = 25) Pageable page) {
        return ApiResponse.success("Leave balances retrieved", service.balances(employeeId, leaveTypeId, year,
                checked(page, Set.of("employeeId", "leaveTypeId", "balanceYear", "updatedAt"), "balanceYear")));
    }
    @GetMapping("/balances/{id}") @PreAuthorize("hasAuthority('leaves.balances.detail')")
    public ApiResponse<BalanceResponse> balance(@PathVariable UUID id) {
        return ApiResponse.success("Leave balance retrieved", service.balance(id));
    }
    @PatchMapping("/balances/{id}/adjust") @PreAuthorize("hasAuthority('leaves.balances.adjust')")
    public ApiResponse<BalanceResponse> adjust(@PathVariable UUID id, @Valid @RequestBody BalanceAdjustment input) {
        return ApiResponse.success("Leave balance adjusted", service.adjustBalance(id, input));
    }

    @PostMapping("/requests") @PreAuthorize("hasAuthority('leaves.requests.create')")
    public ApiResponse<RequestResponse> createRequest(@Valid @RequestBody RequestInput input) {
        return ApiResponse.success("Leave draft created", service.createRequest(input));
    }
    @GetMapping("/requests") @PreAuthorize("hasAuthority('leaves.requests.list')")
    public ApiResponse<Page<RequestResponse>> requests(@RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID leaveTypeId, @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @ParameterObject @PageableDefault(size = 25) Pageable page) {
        return ApiResponse.success("Leave requests retrieved", service.requests(employeeId, leaveTypeId, status,
                from, to, checked(page, Set.of("employeeId", "leaveTypeId", "status", "startDate", "endDate", "createdAt"), "createdAt")));
    }
    @GetMapping("/requests/{id}") @PreAuthorize("hasAuthority('leaves.requests.detail')")
    public ApiResponse<RequestResponse> request(@PathVariable UUID id) {
        return ApiResponse.success("Leave request retrieved", service.request(id));
    }
    @PutMapping("/requests/{id}") @PreAuthorize("hasAuthority('leaves.requests.update')")
    public ApiResponse<RequestResponse> updateRequest(@PathVariable UUID id, @Valid @RequestBody RequestInput input) {
        return ApiResponse.success("Leave draft updated", service.updateRequest(id, input));
    }
    @PatchMapping("/requests/{id}/submit") @PreAuthorize("hasAuthority('leaves.requests.submit')")
    public ApiResponse<RequestResponse> submit(@PathVariable UUID id) {
        return ApiResponse.success("Leave request submitted", service.submit(id));
    }
    @PatchMapping("/requests/{id}/approve") @PreAuthorize("hasAuthority('leaves.requests.approve')")
    public ApiResponse<RequestResponse> approve(@PathVariable UUID id, @RequestBody(required = false) ReviewInput input) {
        return ApiResponse.success("Leave request approved", service.approve(id, input == null ? null : input.comment()));
    }
    @PatchMapping("/requests/{id}/reject") @PreAuthorize("hasAuthority('leaves.requests.reject')")
    public ApiResponse<RequestResponse> reject(@PathVariable UUID id, @RequestBody(required = false) ReviewInput input) {
        return ApiResponse.success("Leave request rejected", service.reject(id, input == null ? null : input.comment()));
    }
    @PatchMapping("/requests/{id}/cancel") @PreAuthorize("hasAuthority('leaves.requests.cancel')")
    public ApiResponse<RequestResponse> cancel(@PathVariable UUID id, @RequestBody(required = false) ReviewInput input) {
        return ApiResponse.success("Leave request cancelled", service.cancel(id, input == null ? null : input.comment()));
    }
    @GetMapping("/requests/{id}/history") @PreAuthorize("hasAuthority('leaves.history.read')")
    public ApiResponse<Page<HistoryResponse>> history(@PathVariable UUID id,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to,
            @RequestParam(required = false) RequestStatus status,
            @ParameterObject @PageableDefault(size = 25) Pageable page) {
        return ApiResponse.success("Leave history retrieved", service.history(id, from, to, status,
                checked(page, Set.of("createdAt", "newStatus"), "createdAt")));
    }

    @PostMapping(value = "/requests/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('leaves.attachments.upload')")
    public ApiResponse<AttachmentResponse> upload(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Leave attachment uploaded", files.upload(id, file));
    }
    @GetMapping("/requests/{id}/attachments") @PreAuthorize("hasAuthority('leaves.attachments.list')")
    public ApiResponse<Page<AttachmentResponse>> attachments(@PathVariable UUID id,
            @ParameterObject @PageableDefault(size = 25) Pageable page) {
        return ApiResponse.success("Leave attachments retrieved", files.list(id,
                checked(page, Set.of("createdAt", "filename", "fileSize"), "createdAt")));
    }
    @GetMapping("/requests/{id}/attachments/{attachmentId}/download")
    @PreAuthorize("hasAuthority('leaves.attachments.download')")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        var result = files.prepareDownload(id, attachmentId);
        StreamingResponseBody body = out -> {
            try { Files.copy(result.path(), out); }
            finally { result.cleanup(); }
        };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.mimeType()))
                .contentLength(result.size()).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(result.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(body);
    }

    private Pageable checked(Pageable page, Set<String> allowed, String fallback) {
        if (page.isUnpaged() || page.getPageNumber() < 0 || page.getPageSize() < 1 || page.getPageSize() > 100)
            throw new BusinessException("Page size must be between 1 and 100");
        for (Sort.Order order : page.getSort()) if (!allowed.contains(order.getProperty()))
            throw new BusinessException("Unsupported sort field: " + order.getProperty());
        Sort sort = page.getSort().isUnsorted() ? Sort.by(fallback).descending() : page.getSort();
        return PageRequest.of(page.getPageNumber(), page.getPageSize(), sort.and(Sort.by("id").ascending()));
    }
}
