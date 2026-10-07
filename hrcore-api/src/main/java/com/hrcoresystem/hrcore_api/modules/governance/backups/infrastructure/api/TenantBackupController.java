package com.hrcoresystem.hrcore_api.modules.governance.backups.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.common.pagination.PaginationSupport;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.service.BackupApplicationService;
import com.hrcoresystem.hrcore_api.modules.governance.backups.application.usecase.*;
import com.hrcoresystem.hrcore_api.modules.governance.backups.domain.model.BackupJob;
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
@RequestMapping("/api/backups")
@SecurityRequirement(name = "bearerAuth")
public class TenantBackupController {

    private final CreateTenantBackupUseCase create;
    private final ListTenantBackupsUseCase list;
    private final GetTenantBackupByIdUseCase get;
    private final RestoreTenantBackupUseCase restore;
    private final RestoreTenantBackupFromFileUseCase restoreFromFile;
    private final BackupApplicationService service;
    private final BackupStorage storage;

    public TenantBackupController(CreateTenantBackupUseCase create, ListTenantBackupsUseCase list, GetTenantBackupByIdUseCase get, RestoreTenantBackupUseCase restore, RestoreTenantBackupFromFileUseCase restoreFromFile, BackupApplicationService service, BackupStorage storage) {
        this.create = create;
        this.list = list;
        this.get = get;
        this.restore = restore;
        this.restoreFromFile = restoreFromFile;
        this.service = service;
        this.storage = storage;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('backups.create')")
    public ResponseEntity<ApiResponse<BackupJobResponse>> createBackup(@Valid @RequestBody(required = false) CreateBackupRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.accepted().body(ApiResponse.success("Tenant backup job accepted", create.execute(reason)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('backups.list')")
    public ApiResponse<Page<BackupJobResponse>> listBackups(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success("Tenant backups retrieved successfully", PaginationSupport.page(list.execute(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('backups.detail')")
    public ApiResponse<BackupJobResponse> getBackupById(@PathVariable UUID id) {
        return ApiResponse.success("Tenant backup retrieved successfully", get.execute(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('backups.download')")
    public ResponseEntity<Resource> downloadBackup(@PathVariable UUID id) {
        BackupJob job = service.registerTenantDownload(id);
        Resource res = storage.loadAsResource(job.storagePath());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.fileName() + "\"").body(res);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('backups.restore')")
    public ResponseEntity<ApiResponse<BackupJobResponse>> restoreBackup(@PathVariable UUID id, @Valid @RequestBody RestoreBackupRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success("Tenant restore job accepted", restore.execute(id, request.confirmationText(), request.reason())));
    }

    @PostMapping(value = "/restore/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('backups.restore')")
    public ResponseEntity<ApiResponse<BackupJobResponse>> restoreBackupFromFile(
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("confirmationText") String confirmationText,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                "Tenant restore file job accepted",
                restoreFromFile.execute(file, confirmationText, reason)
        ));
    }
}
