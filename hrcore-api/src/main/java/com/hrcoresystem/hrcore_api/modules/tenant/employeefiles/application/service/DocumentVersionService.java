package com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.service;

import com.hrcoresystem.hrcore_api.common.exception.*;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.application.dto.VersionResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.model.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.*;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage.DocumentStorageProperties;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentVersionService {
  private static final Logger LOG = LoggerFactory.getLogger(DocumentVersionService.class);
  private final EmployeeDocumentRepository documents;
  private final EmployeeDocumentVersionRepository versions;
  private final DocumentStoragePort storage;
  private final DocumentStorageProperties properties;
  private final EmployeeFilePolicy policy;

  public DocumentVersionService(
      EmployeeDocumentRepository documents,
      EmployeeDocumentVersionRepository versions,
      DocumentStoragePort storage,
      DocumentStorageProperties properties,
      EmployeeFilePolicy policy) {
    this.documents = documents;
    this.versions = versions;
    this.storage = storage;
    this.properties = properties;
    this.policy = policy;
  }

  @Transactional
  public VersionResponse upload(UUID fileId, UUID documentId, MultipartFile upload) {
    policy.operational();
    var file = policy.lockedFile(fileId);
    policy.active(file);
    var document = policy.lockedDocument(fileId, documentId);
    policy.confidential(document);
    policy.active(document);
    String name = filename(upload);
    if (upload.isEmpty() || upload.getSize() > properties.getMaxBytes())
      throw new BusinessException("Invalid document size");
    Path stage = null;
    try {
      stage = Files.createTempFile("hrcore-document-upload-", ".bin");
      var digest = sha256();
      long size = 0;
      try (InputStream in = upload.getInputStream();
          OutputStream out = Files.newOutputStream(stage)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
          size += read;
          if (size > properties.getMaxBytes())
            throw new BusinessException("Document exceeds size limit");
          digest.update(buffer, 0, read);
          out.write(buffer, 0, read);
        }
      }
      if (size == 0) throw new BusinessException("Document is empty");
      String mime = detect(stage, name, upload.getContentType());
      String checksum = HexFormat.of().formatHex(digest.digest());
      UUID versionId = UUID.randomUUID();
      int number = versions.nextVersionNumber(documentId);
      String key = key(file.employeeId(), documentId, versionId);
      if (!TransactionSynchronizationManager.isSynchronizationActive())
        throw new IllegalStateException("Upload transaction is required");
      storage.put(key, stage, mime, checksum);
      registerRollbackCleanup(key, versionId);
      var saved =
          versions.save(
              new EmployeeDocumentVersion(
                  versionId,
                  documentId,
                  number,
                  name,
                  key,
                  mime,
                  size,
                  checksum,
                  policy.actor(),
                  Instant.now()));
      if (document.verificationStatus() != VerificationStatus.PENDING)
        documents.save(
            new EmployeeDocument(
                document.id(),
                document.employeeFileId(),
                document.contractId(),
                document.documentType(),
                document.title(),
                document.description(),
                document.documentNumber(),
                document.issuedAt(),
                document.expiresAt(),
                document.confidential(),
                VerificationStatus.PENDING,
                null,
                null,
                document.status(),
                document.createdBy(),
                document.createdAt(),
                document.updatedAt()));
      policy.audit(
          "EMPLOYEE_DOCUMENT_VERSION_UPLOADED",
          "EMPLOYEE_DOCUMENT_VERSION",
          versionId,
          Map.of("documentId", documentId, "versionNumber", number, "size", size));
      return map(saved);
    } catch (IOException ex) {
      throw new IllegalStateException("Document staging failed", ex);
    } finally {
      if (stage != null)
        try {
          Files.deleteIfExists(stage);
        } catch (IOException ex) {
          LOG.warn("Document staging cleanup failed");
        }
    }
  }

  @Transactional
  public Page<VersionResponse> list(UUID fileId, UUID documentId, Pageable pageable) {
    policy.operational();
    policy.file(fileId);
    var document = policy.document(fileId, documentId);
    policy.confidential(document);
    var page = versions.findAll(documentId, pageable).map(this::map);
    if (document.confidential())
      policy.audit(
          "CONFIDENTIAL_VERSIONS_LISTED",
          "EMPLOYEE_DOCUMENT",
          documentId,
          Map.of("count", page.getNumberOfElements()));
    return page;
  }

  @Transactional
  public Download prepareDownload(UUID fileId, UUID documentId, UUID versionId) {
    policy.operational();
    policy.file(fileId);
    var document = policy.document(fileId, documentId);
    policy.confidential(document);
    var version =
        versions
            .findById(versionId)
            .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
    if (!version.documentId().equals(documentId))
      throw new ResourceNotFoundException("Version not found");
    Path dir = null;
    try {
      dir = Files.createTempDirectory("hrcore-document-download-");
      Path target = dir.resolve("payload");
      storage.get(version.storageKey(), target, properties.getMaxBytes());
      long actual = Files.size(target);
      if (actual != version.fileSize() || actual > properties.getMaxBytes())
        throw new IllegalStateException("Stored document size mismatch");
      var digest = sha256();
      try (InputStream in = Files.newInputStream(target)) {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
      }
      if (!HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(version.checksumSha256()))
        throw new IllegalStateException("Stored document checksum mismatch");
      policy.audit(
          "EMPLOYEE_DOCUMENT_DOWNLOAD_PREPARED",
          "EMPLOYEE_DOCUMENT_VERSION",
          versionId,
          Map.of("documentId", documentId, "confidential", document.confidential()));
      return new Download(target, version.originalFilename(), version.mimeType(), actual);
    } catch (IOException ex) {
      cleanup(dir);
      throw new IllegalStateException("Document download failed", ex);
    } catch (RuntimeException ex) {
      cleanup(dir);
      throw ex;
    }
  }

  private void registerRollbackCleanup(String key, UUID versionId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive())
      throw new IllegalStateException("Upload transaction is required");
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED)
              try {
                storage.delete(key);
              } catch (RuntimeException ex) {
                LOG.error("Compensation failed for document version {}", versionId, ex);
              }
          }
        });
  }

  private String key(UUID employeeId, UUID documentId, UUID versionId) {
    String tenant = TenantContextHolder.getRequired().tenantSlug();
    if (!tenant.matches("[a-zA-Z0-9_-]+"))
      throw new BusinessException("Invalid tenant storage prefix");
    return "tenants/"
        + tenant
        + "/employees/"
        + employeeId
        + "/documents/"
        + documentId
        + "/versions/"
        + versionId;
  }

  private String filename(MultipartFile upload) {
    String name = upload.getOriginalFilename();
    if (name == null
        || name.isBlank()
        || name.length() > 255
        || name.contains("/")
        || name.contains("\\")
        || name.chars().anyMatch(ch -> ch < 32 || ch == 127))
      throw new BusinessException("Invalid filename");
    return name.trim();
  }

  private String detect(Path path, String name, String declared) throws IOException {
    byte[] prefix;
    try (InputStream in = Files.newInputStream(path)) {
      prefix = in.readNBytes(8);
    }
    String mime;
    if (prefix.length >= 5
        && prefix[0] == '%'
        && prefix[1] == 'P'
        && prefix[2] == 'D'
        && prefix[3] == 'F'
        && prefix[4] == '-') mime = "application/pdf";
    else if (prefix.length >= 3
        && (prefix[0] & 255) == 255
        && (prefix[1] & 255) == 216
        && (prefix[2] & 255) == 255) mime = "image/jpeg";
    else if (prefix.length >= 8
        && Arrays.equals(prefix, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
      mime = "image/png";
    else throw new BusinessException("Unsupported document content");
    String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    boolean matches =
        switch (mime) {
          case "application/pdf" -> ext.equals("pdf");
          case "image/jpeg" -> ext.equals("jpg") || ext.equals("jpeg");
          default -> ext.equals("png");
        };
    if (!matches || !mime.equalsIgnoreCase(declared))
      throw new BusinessException("Filename, MIME type and content do not match");
    return mime;
  }

  private MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private VersionResponse map(EmployeeDocumentVersion v) {
    return new VersionResponse(
        v.id(),
        v.documentId(),
        v.versionNumber(),
        v.originalFilename(),
        v.mimeType(),
        v.fileSize(),
        v.checksumSha256(),
        v.createdAt());
  }

  private void cleanup(Path dir) {
    if (dir == null) return;
    try {
      Files.deleteIfExists(dir.resolve("payload"));
      Files.deleteIfExists(dir);
    } catch (IOException ex) {
      LOG.warn("Temporary download cleanup failed");
    }
  }

  public record Download(Path path, String filename, String mimeType, long size) {
    public void cleanup() {
      try {
        Files.deleteIfExists(path);
        Files.deleteIfExists(path.getParent());
      } catch (IOException ex) {
        LOG.warn("Temporary download cleanup failed");
      }
    }
  }
}
