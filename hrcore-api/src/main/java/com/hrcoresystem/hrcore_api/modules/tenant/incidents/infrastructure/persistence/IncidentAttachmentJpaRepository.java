package com.hrcoresystem.hrcore_api.modules.tenant.incidents.infrastructure.persistence;
import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface IncidentAttachmentJpaRepository extends JpaRepository<IncidentAttachmentEntity,UUID>{Page<IncidentAttachmentEntity> findByIncidentId(UUID incidentId,Pageable page);}
