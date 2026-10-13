package com.hrcoresystem.hrcore_api.modules.tenant.attendance.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AttendancePunchJpaRepository extends JpaRepository<AttendancePunchEntity, UUID>,
        JpaSpecificationExecutor<AttendancePunchEntity> {
    @Query("select p from AttendancePunchEntity p where p.employeeId = :employeeId " +
            "and p.punchedAt >= :from and p.punchedAt < :to order by p.punchedAt, p.id")
    List<AttendancePunchEntity> findInWindow(@Param("employeeId") UUID employeeId,
            @Param("from") Instant from, @Param("to") Instant to);

    List<AttendancePunchEntity> findByAttendanceRecordIdOrderByPunchedAtAscIdAsc(UUID recordId);
}
