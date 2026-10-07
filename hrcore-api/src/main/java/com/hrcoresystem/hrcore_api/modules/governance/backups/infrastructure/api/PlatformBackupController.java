package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.service.BackupApplicationService;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupJob;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupScope;
import com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.storage.BackupStorage;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/backups")
@SecurityRequirement(name = "bearerAuth")
public class PlatformBackupController {

    private final CreatePlatformFullBackupUseCase full;
    private final CreatePlatformTenantBackupUseCase tenant;
    private final ListPlatformBackupsUseCase list;
    private final GetPlatformBackupByIdUseCase get;
    private final RestorePlatformBackupUseCase restore;
    private final RestorePlatformBackupFromFileUseCase restoreFromFile;
    private final BackupApplicationService service;
    private final BackupStorage storage;

    public PlatformBackupController(CreatePlatformFullBackupUseCase full, CreatePlatformTenantBackupUseCase tenant, ListPlatformBackupsUseCase list, GetPlatformBackupByIdUseCase get, RestorePlatformBackupUseCase restore, RestorePlatformBackupFromFileUseCase restoreFromFile, BackupApplicationService service, BackupStorage storage) {
        this.full = full;
        this.tenant = tenant;
        this.list = list;
        this.get = get;
        this.restore = restore;
        this.restoreFromFile = restoreFromFile;
        this.service = service;
        this.storage = storage;
    }

    @PostMapping("/full")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<BackupJobResponse>> createFullBackup(@Valid @RequestBody(required = false) CreateBackupRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.accepted().body(ApiResponse.success("Full database backup job accepted", full.execute(reason)));
    }

    @PostMapping("/tenants/{tenantId}")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<BackupJobResponse>> createTenantBackup(@PathVariable UUID tenantId, @Valid @RequestBody(required = false) CreateBackupRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.accepted().body(ApiResponse.success("Tenant backup job accepted", tenant.execute(tenantId, reason)));
    }

    @GetMapping
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<Page<BackupJobResponse>> listBackups(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Platform backups retrieved successfully", PaginationSupport.page(list.execute(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ApiResponse<BackupJobResponse> getBackupById(@PathVariable UUID id) {
        return ApiResponse.success("Platform backup retrieved successfully", get.execute(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ResponseEntity<Resource> downloadBackup(@PathVariable UUID id) {
        BackupJob job = service.registerPlatformDownload(id);
        Resource res = storage.loadAsResource(job.storagePath());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.fileName() + "\"").body(res);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<BackupJobResponse>> restoreBackup(@PathVariable UUID id, @Valid @RequestBody RestoreBackupRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success("Platform restore job accepted", restore.execute(id, request.confirmationText(), request.reason())));
    }

    @PostMapping(value = "/restore/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authorizationGuards.isPlatformAdmin()")
    public ResponseEntity<ApiResponse<BackupJobResponse>> restoreBackupFromFile(
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("scope") BackupScope scope,
            @RequestParam(value = "tenantId", required = false) UUID tenantId,
            @RequestParam("confirmationText") String confirmationText,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                "Platform restore file job accepted",
                restoreFromFile.execute(file, scope, tenantId, confirmationText, reason)
        ));
    }
}
