package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AttendanceAdjustmentJpaRepository extends JpaRepository<AttendanceAdjustmentEntity, UUID>,
        JpaSpecificationExecutor<AttendanceAdjustmentEntity> {
    List<AttendanceAdjustmentEntity> findByAttendanceRecordIdOrderByCreatedAtAscIdAsc(UUID recordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AttendanceAdjustmentEntity a where a.id = :id")
    Optional<AttendanceAdjustmentEntity> lockById(@Param("id") UUID id);
}
