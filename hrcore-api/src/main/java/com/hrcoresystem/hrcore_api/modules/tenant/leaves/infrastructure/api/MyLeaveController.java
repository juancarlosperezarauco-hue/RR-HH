package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.application.service.EmployeeSupervisionScopeService;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.dto.LeaveDtos.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.LeaveService;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service.LeaveAttachmentService;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.RequestStatus;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me/leaves")
public class MyLeaveController {
    private final LeaveService service;
    private final EmployeeSupervisionScopeService scope;
    private final LeaveAttachmentService files;
    public MyLeaveController(LeaveService service, EmployeeSupervisionScopeService scope, LeaveAttachmentService files) {
        this.service = service; this.scope = scope; this.files = files;
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAuthority('leaves.balances.list')")
    public ApiResponse<Page<BalanceResponse>> balances(@RequestParam(required = false) UUID leaveTypeId,
            @RequestParam(required = false) Integer year, @PageableDefault(size = 25) Pageable page) {
        return ApiResponse.success("Saldos propios consultados correctamente",
                service.balances(scope.requireCurrentEmployeeId(), leaveTypeId, year, page));
    }

    @GetMapping({"", "/leave-balances"})
    @PreAuthorize("hasAuthority('leaves.balances.list')")
    public ApiResponse<Page<BalanceResponse>> balanceAlias(@RequestParam(required = false) UUID leaveTypeId,
            @RequestParam(required = false) Integer year, @PageableDefault(size = 25) Pageable page) {
        return balances(leaveTypeId, year, page);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('leaves.requests.list')")
    public ApiResponse<Page<RequestResponse>> requests(@RequestParam(required = false) UUID leaveTypeId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @PageableDefault(size = 25) Pageable page) {
        return ApiResponse.success("Solicitudes propias consultadas correctamente",
                service.requests(scope.requireCurrentEmployeeId(), leaveTypeId, status, from, to, page));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('leaves.requests.create')")
    public ApiResponse<RequestResponse> create(@Valid @RequestBody SelfRequestInput input) {
        UUID employeeId = scope.requireCurrentEmployeeId();
        return ApiResponse.success("Borrador de ausencia creado correctamente", service.createRequest(
                new RequestInput(employeeId, input.leaveTypeId(), input.startDate(), input.endDate(),
                        input.startTime(), input.endTime(), input.reason())));
    }

    @PutMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('leaves.requests.update')")
    public ApiResponse<RequestResponse> update(@PathVariable UUID id, @Valid @RequestBody SelfRequestInput input) {
        RequestResponse current = service.request(id); verifyOwner(current);
        UUID employeeId = scope.requireCurrentEmployeeId();
        return ApiResponse.success("Borrador de ausencia actualizado correctamente", service.updateRequest(id,
                new RequestInput(employeeId, input.leaveTypeId(), input.startDate(), input.endDate(),
                        input.startTime(), input.endTime(), input.reason())));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('leaves.requests.detail')")
    public ApiResponse<RequestResponse> detail(@PathVariable UUID id) {
        RequestResponse result = service.request(id);
        if (!result.employeeId().equals(scope.requireCurrentEmployeeId()))
            throw new org.springframework.security.access.AccessDeniedException("La solicitud no pertenece al empleado autenticado");
        return ApiResponse.success("Solicitud propia consultada correctamente", result);
    }

    @PatchMapping("/requests/{id}/submit")
    @PreAuthorize("hasAuthority('leaves.requests.submit')")
    public ApiResponse<RequestResponse> submit(@PathVariable UUID id) {
        RequestResponse result = service.request(id);
        verifyOwner(result);
        return ApiResponse.success("Solicitud enviada correctamente", service.submit(id));
    }

    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAuthority('leaves.requests.cancel')")
    public ApiResponse<RequestResponse> cancel(@PathVariable UUID id, @RequestBody(required = false) ReviewInput input) {
        RequestResponse result = service.request(id);
        verifyOwner(result);
        return ApiResponse.success("Solicitud cancelada correctamente", service.cancel(id, input == null ? null : input.comment()));
    }

    @PostMapping(value = "/requests/{id}/attachments", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('leaves.attachments.upload')")
    public ApiResponse<AttachmentResponse> upload(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        verifyOwner(service.request(id));
        return ApiResponse.success("Adjunto de ausencia cargado correctamente", files.upload(id, file));
    }

    @GetMapping("/requests/{id}/attachments")
    @PreAuthorize("hasAuthority('leaves.attachments.list')")
    public ApiResponse<Page<AttachmentResponse>> attachments(@PathVariable UUID id,
            @PageableDefault(size = 25) Pageable page) {
        verifyOwner(service.request(id));
        return ApiResponse.success("Adjuntos propios consultados correctamente", files.list(id, page));
    }

    private void verifyOwner(RequestResponse result) {
        if (!result.employeeId().equals(scope.requireCurrentEmployeeId()))
            throw new org.springframework.security.access.AccessDeniedException("La solicitud no pertenece al empleado autenticado");
    }
}
