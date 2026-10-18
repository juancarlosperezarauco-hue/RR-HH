package com.hrcoresystem.hrcore_api.modules.tenant.incidents.infrastructure.persistence;
import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface IncidentActionJpaRepository extends JpaRepository<IncidentActionEntity,UUID> { List<IncidentActionEntity> findByIncidentIdOrderByCreatedAtAsc(UUID id); @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from IncidentActionEntity a where a.id=:id") Optional<IncidentActionEntity> lock(@Param("id") UUID id); }
