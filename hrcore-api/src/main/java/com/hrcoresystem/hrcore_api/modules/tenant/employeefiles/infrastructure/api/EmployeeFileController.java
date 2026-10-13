package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.api;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/employee-files")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeFileController {
  private final EmployeeFileManagementService files;
  private final EmployeeDocumentManagementService documents;
  private final DocumentVersionService versions;

  public EmployeeFileController(
      EmployeeFileManagementService files,
      EmployeeDocumentManagementService documents,
      DocumentVersionService versions) {
    this.files = files;
    this.documents = documents;
    this.versions = versions;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('employee_files.create')")
  public ApiResponse<FileResponse> create(@Valid @RequestBody CreateFileRequest r) {
    return ApiResponse.success("Employee file created", files.create(r));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('employee_files.list')")
  public ApiResponse<Page<FileResponse>> list(
      @RequestParam(required = false) UUID employeeId,
      @RequestParam(required = false) String fileNumber,
      @RequestParam(required = false) FileStatus status,
      @ParameterObject @PageableDefault(size = 25) Pageable page) {
    return ApiResponse.success(
        "Employee files retrieved",
        files.list(
            new EmployeeFileFilter(employeeId, fileNumber, status),
            checked(page, Set.of("fileNumber", "status", "createdAt"), "createdAt")));
  }

  @GetMapping("/by-employee/{employeeId}")
  @PreAuthorize("hasAuthority('employee_files.detail')")
  public ApiResponse<FileResponse> byEmployee(@PathVariable UUID employeeId) {
    return ApiResponse.success("Employee file retrieved", files.byEmployee(employeeId));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('employee_files.detail')")
  public ApiResponse<FileResponse> get(@PathVariable UUID id) {
    return ApiResponse.success("Employee file retrieved", files.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('employee_files.update')")
  public ApiResponse<FileResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateFileRequest r) {
    return ApiResponse.success("Employee file updated", files.update(id, r));
  }

  @PostMapping("/{id}/documents")
  @PreAuthorize("hasAuthority('employee_files.documents.create')")
  public ApiResponse<DocumentResponse> createDocument(
      @PathVariable UUID id, @Valid @RequestBody CreateDocumentRequest r) {
    return ApiResponse.success("Document created", documents.create(id, r));
  }

  @GetMapping("/{id}/documents")
  @PreAuthorize("hasAuthority('employee_files.documents.list')")
  public ApiResponse<Page<DocumentResponse>> listDocuments(
      @PathVariable UUID id,
      @RequestParam(required = false) DocumentType type,
      @RequestParam(required = false) FileStatus status,
      @RequestParam(required = false) LocalDate expiresBefore,
      @ParameterObject @PageableDefault(size = 25) Pageable page) {
    return ApiResponse.success(
        "Documents retrieved",
        documents.list(
            id,
            new EmployeeDocumentFilter(type, status, expiresBefore, false),
            checked(
                page,
                Set.of("title", "documentType", "status", "issuedAt", "expiresAt", "createdAt"),
                "createdAt")));
  }

  @GetMapping("/{id}/documents/{documentId}")
  @PreAuthorize("hasAuthority('employee_files.documents.detail')")
  public ApiResponse<DocumentResponse> getDocument(
      @PathVariable UUID id, @PathVariable UUID documentId) {
    return ApiResponse.success("Document retrieved", documents.get(id, documentId));
  }

  @PutMapping("/{id}/documents/{documentId}")
  @PreAuthorize("hasAuthority('employee_files.documents.update')")
  public ApiResponse<DocumentResponse> updateDocument(
      @PathVariable UUID id,
      @PathVariable UUID documentId,
      @Valid @RequestBody UpdateDocumentRequest r) {
    return ApiResponse.success("Document updated", documents.update(id, documentId, r));
  }

  @PatchMapping("/{id}/documents/{documentId}/verify")
  @PreAuthorize("hasAuthority('employee_files.documents.verify')")
  public ApiResponse<DocumentResponse> verify(
      @PathVariable UUID id,
      @PathVariable UUID documentId,
      @Valid @RequestBody VerificationRequest r) {
    return ApiResponse.success(
        "Document verification updated", documents.verify(id, documentId, r.status()));
  }

  @PostMapping(
      value = "/{id}/documents/{documentId}/versions",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('employee_files.documents.upload')")
  public ApiResponse<VersionResponse> upload(
      @PathVariable UUID id,
      @PathVariable UUID documentId,
      @RequestPart("file") MultipartFile file) {
    return ApiResponse.success("Document version uploaded", versions.upload(id, documentId, file));
  }

  @GetMapping("/{id}/documents/{documentId}/versions")
  @PreAuthorize("hasAuthority('employee_files.documents.detail')")
  public ApiResponse<Page<VersionResponse>> listVersions(
      @PathVariable UUID id,
      @PathVariable UUID documentId,
      @ParameterObject @PageableDefault(size = 25) Pageable page) {
    return ApiResponse.success(
        "Versions retrieved",
        versions.list(
            id, documentId, checked(page, Set.of("versionNumber", "createdAt"), "versionNumber")));
  }

  @GetMapping("/{id}/documents/{documentId}/versions/{versionId}/download")
  @PreAuthorize("hasAuthority('employee_files.documents.download')")
  public ResponseEntity<StreamingResponseBody> download(
      @PathVariable UUID id, @PathVariable UUID documentId, @PathVariable UUID versionId) {
    var result = versions.prepareDownload(id, documentId, versionId);
    StreamingResponseBody body =
        out -> {
          try {
            Files.copy(result.path(), out);
          } finally {
            result.cleanup();
          }
        };
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(result.mimeType()))
        .contentLength(result.size())
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(body);
  }

  private Pageable checked(Pageable p, Set<String> allowed, String fallback) {
    if (p.isUnpaged() || p.getPageNumber() < 0 || p.getPageSize() < 1 || p.getPageSize() > 100)
      throw new BusinessException("Page size must be between 1 and 100");
    for (Sort.Order o : p.getSort())
      if (!allowed.contains(o.getProperty()))
        throw new BusinessException("Unsupported sort field: " + o.getProperty());
    Sort sort = p.getSort().isUnsorted() ? Sort.by(fallback).descending() : p.getSort();
    return PageRequest.of(p.getPageNumber(), p.getPageSize(), sort.and(Sort.by("id").ascending()));
  }
}
