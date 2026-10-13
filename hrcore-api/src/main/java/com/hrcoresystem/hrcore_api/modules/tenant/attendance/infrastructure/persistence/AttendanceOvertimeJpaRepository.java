package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AttendanceOvertimeJpaRepository extends JpaRepository<AttendanceOvertimeEntity, UUID>,
        JpaSpecificationExecutor<AttendanceOvertimeEntity> {
    List<AttendanceOvertimeEntity> findByAttendanceRecordIdOrderByCreatedAtAscIdAsc(UUID recordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AttendanceOvertimeEntity o where o.id = :id")
    Optional<AttendanceOvertimeEntity> lockById(@Param("id") UUID id);
}
