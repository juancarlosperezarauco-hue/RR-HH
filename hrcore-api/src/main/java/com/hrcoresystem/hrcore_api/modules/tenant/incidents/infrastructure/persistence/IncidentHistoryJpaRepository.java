package com.hrcoresystem.hrcore_api.modules.tenant.incidents.infrastructure.persistence;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface IncidentHistoryJpaRepository extends JpaRepository<IncidentHistoryEntity,UUID>{List<IncidentHistoryEntity> findByIncidentIdOrderByCreatedAtAscIdAsc(UUID incidentId);}
