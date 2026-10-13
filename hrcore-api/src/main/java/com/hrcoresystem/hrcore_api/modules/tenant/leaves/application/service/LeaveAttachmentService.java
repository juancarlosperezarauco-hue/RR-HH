package com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.repository.EmployeeRepository;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.DocumentStoragePort;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage.DocumentStorageProperties;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.application.dto.LeaveDtos.AttachmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.*;
import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.repository.LeaveStore;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LeaveAttachmentService {
    private static final Logger LOG = LoggerFactory.getLogger(LeaveAttachmentService.class);
    private final LeaveStore store;
    private final EmployeeRepository employees;
    private final DocumentStoragePort storage;
    private final DocumentStorageProperties properties;
    private final TenantPlanEnforcementService plans;
    private final SecurityContextFacade security;
    private final AuditTrailService audit;

    public LeaveAttachmentService(LeaveStore store, EmployeeRepository employees, DocumentStoragePort storage,
            DocumentStorageProperties properties, TenantPlanEnforcementService plans,
            SecurityContextFacade security, AuditTrailService audit) {
        this.store = store; this.employees = employees; this.storage = storage; this.properties = properties;
        this.plans = plans; this.security = security; this.audit = audit;
    }

    @Transactional public AttachmentResponse upload(UUID requestId, MultipartFile file) {
        plans.assertCanOperate(); LeaveRequest observed = request(requestId);
        employees.findByIdForUpdate(observed.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        LeaveRequest request = store.lockedRequest(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        if (request.status() != RequestStatus.DRAFT && request.status() != RequestStatus.PENDING)
            throw new BusinessException("Attachments can be added only before review");
        String name = filename(file);
        if (file.isEmpty() || file.getSize() > properties.getMaxBytes())
            throw new BusinessException("Invalid attachment size");
        Path stage = null;
        try {
            stage = Files.createTempFile("hrcore-leave-upload-", ".bin");
            MessageDigest digest = digest(); long size = 0;
            try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(stage)) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) != -1) {
                    size += n;
                    if (size > properties.getMaxBytes()) throw new BusinessException("Attachment exceeds size limit");
                    digest.update(buffer, 0, n); out.write(buffer, 0, n);
                }
            }
            if (size == 0) throw new BusinessException("Attachment is empty");
            String mime = detect(stage, name, file.getContentType());
            UUID id = UUID.randomUUID(); String key = key(request.employeeId(), requestId, id);
            if (!TransactionSynchronizationManager.isSynchronizationActive())
                throw new IllegalStateException("Attachment upload transaction is required");
            storage.put(key, stage, mime, HexFormat.of().formatHex(digest.digest()));
            registerRollbackCleanup(key, id);
            LeaveAttachment saved = store.save(new LeaveAttachment(id, requestId, name, key, mime, size,
                    actor(), null));
            audit.recordTenantEvent("LEAVE_ATTACHMENT_UPLOADED", "LEAVE_ATTACHMENT", id.toString(),
                    Map.of("requestId", requestId, "size", size));
            return map(saved);
        } catch (IOException ex) {
            throw new IllegalStateException("Attachment staging failed", ex);
        } finally {
            if (stage != null) try { Files.deleteIfExists(stage); }
            catch (IOException ex) { LOG.warn("Leave attachment staging cleanup failed"); }
        }
    }

    @Transactional(readOnly = true) public Page<AttachmentResponse> list(UUID requestId, Pageable page) {
        plans.assertCanOperate(); request(requestId);
        return store.attachments(requestId, page).map(this::map);
    }

    /** Confirms that at least one attachment still exists privately and matches its V16 metadata. */
    public void requireStoredAttachment(UUID requestId) {
        if (!store.hasAttachment(requestId)) throw new BusinessException("A stored attachment is required");
        int pageNumber = 0;
        Page<LeaveAttachment> page;
        do {
            page = store.attachments(requestId, PageRequest.of(pageNumber++, 25,
                    Sort.by("createdAt").ascending().and(Sort.by("id"))));
            for (LeaveAttachment attachment : page.getContent()) {
                if (isStoredAndValid(attachment)) return;
            }
        } while (page.hasNext());
        throw new BusinessException("No valid stored attachment is available");
    }

    @Transactional public Download prepareDownload(UUID requestId, UUID attachmentId) {
        plans.assertCanOperate(); request(requestId);
        LeaveAttachment attachment = store.attachment(attachmentId)
                .filter(a -> a.leaveRequestId().equals(requestId))
                .orElseThrow(() -> new ResourceNotFoundException("Leave attachment not found"));
        String tenant = TenantContextHolder.getRequired().tenantSlug();
        if (!attachment.storageKey().startsWith("tenants/" + tenant + "/"))
            throw new ResourceNotFoundException("Leave attachment not found");
        Path dir = null;
        try {
            dir = Files.createTempDirectory("hrcore-leave-download-");
            Path target = dir.resolve("payload");
            storage.get(attachment.storageKey(), target, properties.getMaxBytes());
            if (Files.size(target) != attachment.fileSize())
                throw new IllegalStateException("Stored attachment size mismatch");
            detect(target, attachment.filename(), attachment.mimeType());
            audit.recordTenantEvent("LEAVE_ATTACHMENT_DOWNLOAD_PREPARED", "LEAVE_ATTACHMENT",
                    attachmentId.toString(), Map.of("requestId", requestId));
            return new Download(target, attachment.filename(), attachment.mimeType(), attachment.fileSize());
        } catch (IOException | RuntimeException ex) {
            cleanup(dir); throw new IllegalStateException("Attachment download failed", ex);
        }
    }

    private LeaveRequest request(UUID id) {
        return store.request(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
    }
    private boolean isStoredAndValid(LeaveAttachment attachment) {
        String tenant = TenantContextHolder.getRequired().tenantSlug();
        if (!attachment.storageKey().startsWith("tenants/" + tenant + "/")) return false;
        Path dir = null;
        try {
            dir = Files.createTempDirectory("hrcore-leave-verify-");
            Path target = dir.resolve("payload");
            storage.get(attachment.storageKey(), target, properties.getMaxBytes());
            return Files.size(target) == attachment.fileSize()
                    && attachment.mimeType().equals(detect(target, attachment.filename(), attachment.mimeType()));
        } catch (IOException | RuntimeException ex) {
            return false;
        } finally { cleanup(dir); }
    }
    private AttachmentResponse map(LeaveAttachment a) {
        return new AttachmentResponse(a.id(), a.leaveRequestId(), a.filename(), a.mimeType(),
                a.fileSize(), a.uploadedBy(), a.createdAt());
    }
    private UUID actor() {
        try { return UUID.fromString(security.getCurrentSubject()); }
        catch (RuntimeException ex) { throw new BusinessException("Authenticated tenant user is required"); }
    }
    private String key(UUID employeeId, UUID requestId, UUID id) {
        String tenant = TenantContextHolder.getRequired().tenantSlug();
        if (!tenant.matches("[a-zA-Z0-9_-]+")) throw new BusinessException("Invalid tenant storage prefix");
        return "tenants/" + tenant + "/employees/" + employeeId + "/leaves/" + requestId + "/attachments/" + id;
    }
    private String filename(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || name.length() > 255 || name.contains("/")
                || name.contains("\\") || name.chars().anyMatch(ch -> ch < 32 || ch == 127))
            throw new BusinessException("Invalid attachment filename");
        return name.trim();
    }
    private String detect(Path path, String name, String declared) throws IOException {
        byte[] prefix;
        try (InputStream in = Files.newInputStream(path)) { prefix = in.readNBytes(8); }
        String mime;
        if (prefix.length >= 5 && prefix[0] == '%' && prefix[1] == 'P' && prefix[2] == 'D'
                && prefix[3] == 'F' && prefix[4] == '-') mime = "application/pdf";
        else if (prefix.length >= 3 && (prefix[0] & 255) == 255 && (prefix[1] & 255) == 216
                && (prefix[2] & 255) == 255) mime = "image/jpeg";
        else if (prefix.length >= 8 && Arrays.equals(prefix,
                new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10})) mime = "image/png";
        else throw new BusinessException("Unsupported attachment content");
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        boolean matches = switch (mime) {
            case "application/pdf" -> ext.equals("pdf");
            case "image/jpeg" -> ext.equals("jpg") || ext.equals("jpeg");
            default -> ext.equals("png");
        };
        if (!matches || !mime.equalsIgnoreCase(declared))
            throw new BusinessException("Filename, MIME type and content do not match");
        return mime;
    }
    private MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private void registerRollbackCleanup(String key, UUID id) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) try { storage.delete(key); }
                catch (RuntimeException ex) { LOG.error("Leave attachment compensation failed for {}", id, ex); }
            }
        });
    }
    private void cleanup(Path dir) {
        if (dir == null) return;
        try { Files.deleteIfExists(dir.resolve("payload")); Files.deleteIfExists(dir); }
        catch (IOException ex) { LOG.warn("Leave attachment temporary download cleanup failed"); }
    }
    public record Download(Path path, String filename, String mimeType, long size) {
        public void cleanup() {
            try { Files.deleteIfExists(path); Files.deleteIfExists(path.getParent()); }
            catch (IOException ex) { LOG.warn("Leave attachment temporary download cleanup failed"); }
        }
    }
}
