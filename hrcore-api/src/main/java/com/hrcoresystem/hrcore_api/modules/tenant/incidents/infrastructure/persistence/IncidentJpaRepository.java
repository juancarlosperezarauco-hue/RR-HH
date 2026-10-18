package com.hrcoresystem.hrcore_api.modules.tenant.incidents.infrastructure.persistence;
import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface IncidentJpaRepository extends JpaRepository<IncidentEntity,UUID>, JpaSpecificationExecutor<IncidentEntity> { @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from IncidentEntity i where i.id=:id") Optional<IncidentEntity> lock(@Param("id") UUID id); }
