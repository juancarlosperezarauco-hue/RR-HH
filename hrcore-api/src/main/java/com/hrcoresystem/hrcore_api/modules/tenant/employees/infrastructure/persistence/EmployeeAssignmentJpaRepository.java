package com.hrcoresystem.hrcore_api.modules.tenant.employees.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface EmployeeAssignmentJpaRepository extends JpaRepository<EmployeeAssignmentEntity, UUID> {
    Optional<EmployeeAssignmentEntity> findByIdAndEmployeeId(UUID id, UUID employeeId);
    Page<EmployeeAssignmentEntity> findByEmployeeId(UUID employeeId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from EmployeeAssignmentEntity a where a.employeeId=:employeeId and a.assignmentType=com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.AssignmentType.PRIMARY and a.assignmentStatus=com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeAssignmentStatus.ACTIVE and a.startDate<=:asOfDate and (a.endDate is null or a.endDate>=:asOfDate) order by a.id")
    List<EmployeeAssignmentEntity> findCurrentPrimaryForUpdate(@Param("employeeId") UUID employeeId,@Param("asOfDate") LocalDate asOfDate);
    @Query("select a from EmployeeAssignmentEntity a where a.employeeId=:employeeId and a.assignmentType=com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.AssignmentType.PRIMARY and a.assignmentStatus=com.hrcoresystem.hrcore_api.modules.tenant.employees.domain.model.EmployeeAssignmentStatus.ACTIVE and a.startDate<=:asOfDate and (a.endDate is null or a.endDate>=:asOfDate) order by a.id")
    List<EmployeeAssignmentEntity> findCurrentPrimary(@Param("employeeId") UUID employeeId,@Param("asOfDate") LocalDate asOfDate);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from EmployeeAssignmentEntity a where a.employeeId=:employeeId order by a.id")
    List<EmployeeAssignmentEntity> findAllByEmployeeIdForUpdate(@Param("employeeId") UUID employeeId);
}
