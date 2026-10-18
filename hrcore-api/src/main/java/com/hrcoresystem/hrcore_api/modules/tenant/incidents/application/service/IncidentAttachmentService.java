package com.hrcoresystem.hrcore_api.modules.tenant.incidents.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.exception.ResourceNotFoundException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.governance.audit.application.service.AuditTrailService;
import com.hrcoresystem.hrcore_api.modules.platform.subscriptions.application.service.TenantPlanEnforcementService;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.domain.repository.DocumentStoragePort;
import com.hrcoresystem.hrcore_api.modules.tenant.employeefiles.infrastructure.storage.DocumentStorageProperties;
import com.hrcoresystem.hrcore_api.modules.tenant.incidents.application.dto.IncidentDtos.AttachmentResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.incidents.domain.model.IncidentModels.*;
import com.hrcoresystem.hrcore_api.modules.tenant.incidents.infrastructure.persistence.*;
import java.io.*; import java.nio.file.*; import java.security.*; import java.util.*;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.transaction.support.*; import org.springframework.web.multipart.MultipartFile;

@Service
public class IncidentAttachmentService {
    private final IncidentJpaRepository incidents; private final IncidentAttachmentJpaRepository attachments; private final IncidentHistoryJpaRepository histories;
    private final DocumentStoragePort storage; private final DocumentStorageProperties properties;
    private final TenantPlanEnforcementService plans; private final SecurityContextFacade security; private final AuditTrailService audit;
    public IncidentAttachmentService(IncidentJpaRepository incidents, IncidentAttachmentJpaRepository attachments,
            IncidentHistoryJpaRepository histories,
            DocumentStoragePort storage, DocumentStorageProperties properties, TenantPlanEnforcementService plans,
            SecurityContextFacade security, AuditTrailService audit) { this.incidents=incidents; this.attachments=attachments; this.histories=histories; this.storage=storage; this.properties=properties; this.plans=plans; this.security=security; this.audit=audit; }
    @Transactional public AttachmentResponse upload(UUID incidentId, AttachmentType type, String description, MultipartFile file) {
        plans.assertCanOperate(); var incident=incidents.lock(incidentId).orElseThrow(()->new ResourceNotFoundException("Incidencia no encontrada"));
        if (incident.getStatus()==Status.CLOSED||incident.getStatus()==Status.CANCELLED) throw new BusinessException("No se pueden agregar evidencias a una incidencia finalizada");
        String name=filename(file); if(file.isEmpty()||file.getSize()>properties.getMaxBytes()) throw new BusinessException("Tamaño de evidencia inválido");
        Path stage=null; try { stage=Files.createTempFile("hrcore-incident-upload-", ".bin"); MessageDigest digest=MessageDigest.getInstance("SHA-256"); long size=0;
            try(InputStream in=file.getInputStream(); OutputStream out=Files.newOutputStream(stage)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){size+=n;if(size>properties.getMaxBytes())throw new BusinessException("La evidencia supera el tamaño permitido");digest.update(buffer,0,n);out.write(buffer,0,n);}}
            String mime=detect(stage,name,file.getContentType()); UUID id=UUID.randomUUID(); String key=key(incident.getEmployeeId(),incidentId,id); storage.put(key,stage,mime,HexFormat.of().formatHex(digest.digest()));
            String storedKey=key; TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(storedKey);}});
            IncidentAttachmentEntity entity=new IncidentAttachmentEntity();entity.setId(id);entity.setIncidentId(incidentId);entity.setAttachmentType(type==null?AttachmentType.EVIDENCE:type);entity.setFilename(name);entity.setStorageKey(key);entity.setMimeType(mime);entity.setFileSize(size);entity.setDescription(description);entity.setUploadedBy(actor());
            var saved=attachments.saveAndFlush(entity);
            IncidentHistoryEntity history = new IncidentHistoryEntity(); history.setIncidentId(incidentId); history.setAction("EVIDENCE_ADDED");
            history.setComment("Evidencia agregada: " + name); history.setPerformedBy(entity.getUploadedBy()); histories.save(history);
            audit.recordTenantEvent("INCIDENT_ATTACHMENT_UPLOADED","EMPLOYEE_INCIDENT",incidentId.toString(),Map.of("attachmentId",id)); return map(saved);
        } catch(IOException|NoSuchAlgorithmException ex){throw new IllegalStateException("No se pudo preparar la evidencia",ex);} finally {if(stage!=null)try{Files.deleteIfExists(stage);}catch(IOException ignored){}}
    }
    @Transactional(readOnly=true) public Page<AttachmentResponse> list(UUID incidentId, Pageable page){plans.assertCanOperate();require(incidentId);return attachments.findByIncidentId(incidentId,page).map(this::map);}
    @Transactional public Download download(UUID incidentId, UUID attachmentId){plans.assertCanOperate();require(incidentId);var a=attachments.findById(attachmentId).filter(x->incidentId.equals(x.getIncidentId())).orElseThrow(()->new ResourceNotFoundException("Evidencia no encontrada"));String tenant=TenantContextHolder.getRequired().tenantSlug();if(!a.getStorageKey().startsWith("tenants/"+tenant+"/"))throw new ResourceNotFoundException("Evidencia no encontrada");try{Path dir=Files.createTempDirectory("hrcore-incident-download-");Path target=dir.resolve("payload");storage.get(a.getStorageKey(),target,properties.getMaxBytes());if(Files.size(target)!=a.getFileSize())throw new IllegalStateException("Tamaño de evidencia inconsistente");return new Download(target,a.getFilename(),a.getMimeType(),a.getFileSize());}catch(IOException ex){throw new IllegalStateException("No se pudo descargar la evidencia",ex);}}
    private void require(UUID id){if(!incidents.existsById(id))throw new ResourceNotFoundException("Incidencia no encontrada");}
    private UUID actor(){try{return UUID.fromString(security.getCurrentSubject());}catch(Exception ex){throw new BusinessException("Usuario autenticado requerido");}}
    private AttachmentResponse map(IncidentAttachmentEntity e){return new AttachmentResponse(e.getId(),e.getIncidentId(),e.getAttachmentType(),e.getFilename(),e.getMimeType(),e.getFileSize(),e.getDescription(),e.getUploadedBy(),e.getCreatedAt());}
    private String key(UUID employee,UUID incident,UUID id){String tenant=TenantContextHolder.getRequired().tenantSlug();if(!tenant.matches("[a-zA-Z0-9_-]+"))throw new BusinessException("Prefijo de tenant inválido");return "tenants/"+tenant+"/employees/"+employee+"/incidents/"+incident+"/attachments/"+id;}
    private String filename(MultipartFile file){String name=file.getOriginalFilename();if(name==null||name.isBlank()||name.length()>255||name.contains("/")||name.contains("\\"))throw new BusinessException("Nombre de archivo inválido");return name.trim();}
    private String detect(Path path,String name,String declared)throws IOException{byte[] prefix;try(InputStream in=Files.newInputStream(path)){prefix=in.readNBytes(8);}String mime;if(prefix.length>=5&&prefix[0]=='%'&&prefix[1]=='P'&&prefix[2]=='D'&&prefix[3]=='F'&&prefix[4]=='-')mime="application/pdf";else if(prefix.length>=3&&(prefix[0]&255)==255&&(prefix[1]&255)==216&&(prefix[2]&255)==255)mime="image/jpeg";else if(prefix.length>=8&&Arrays.equals(prefix,new byte[]{(byte)137,80,78,71,13,10,26,10}))mime="image/png";else throw new BusinessException("Contenido de evidencia no soportado");String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);if(!(mime.equals("application/pdf")&&ext.equals("pdf")||mime.equals("image/jpeg")&&(ext.equals("jpg")||ext.equals("jpeg"))||mime.equals("image/png")&&ext.equals("png"))||!mime.equalsIgnoreCase(declared))throw new BusinessException("Nombre, MIME y contenido no coinciden");return mime;}
    public record Download(Path path,String filename,String mimeType,long size){public void cleanup(){try{Files.deleteIfExists(path);Files.deleteIfExists(path.getParent());}catch(IOException ignored){}}}
}
