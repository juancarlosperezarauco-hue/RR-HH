package com.hrcoresystem.hrcore_api.modules.tenant.leaves.infrastructure.persistence;

import com.hrcoresystem.hrcore_api.modules.tenant.leaves.domain.model.LeaveModels.RequestStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface LeaveTypeJpaRepository extends JpaRepository<LeaveTypeEntity, UUID>, JpaSpecificationExecutor<LeaveTypeEntity> {
    boolean existsByCodeAndIdNot(String code, UUID id);
    boolean existsByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from LeaveTypeEntity t where t.id=:id")
    Optional<LeaveTypeEntity> lock(@Param("id") UUID id);
}

interface LeaveBalanceJpaRepository extends JpaRepository<LeaveBalanceEntity, UUID>, JpaSpecificationExecutor<LeaveBalanceEntity> {
    boolean existsByEmployeeIdAndLeaveTypeIdAndBalanceYear(UUID employeeId, UUID leaveTypeId, int year);
    @Query(value = "select b.id, coalesce(sum(r.requested_days), 0) from leave_balances b " +
            "join leave_types t on t.id=b.leave_type_id " +
            "left join leave_requests r on r.employee_id=b.employee_id and r.leave_type_id=b.leave_type_id " +
            "and r.status='PENDING' and extract(year from r.start_date)=b.balance_year " +
            "where b.id in (:ids) and t.deducts_balance=true group by b.id", nativeQuery = true)
    List<Object[]> pendingForBalances(@Param("ids") List<UUID> ids);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from LeaveBalanceEntity b where b.employeeId=:employee and b.leaveTypeId=:type and b.balanceYear=:year")
    Optional<LeaveBalanceEntity> lock(@Param("employee") UUID employee, @Param("type") UUID type, @Param("year") int year);
}

interface LeaveRequestJpaRepository extends JpaRepository<LeaveRequestEntity, UUID>, JpaSpecificationExecutor<LeaveRequestEntity> {
    boolean existsByLeaveTypeIdAndStatusIn(UUID typeId, List<RequestStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LeaveRequestEntity r where r.id=:id")
    Optional<LeaveRequestEntity> lock(@Param("id") UUID id);
    @Query("select coalesce(sum(r.requestedDays), 0) from LeaveRequestEntity r, LeaveTypeEntity t where t.id=r.leaveTypeId and t.deductsBalance=true and r.employeeId=:employee and r.leaveTypeId=:type and r.status=:status and r.startDate between :from and :to")
    BigDecimal pending(@Param("employee") UUID employee, @Param("type") UUID type,
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("status") RequestStatus status);
    @Query("select r from LeaveRequestEntity r where r.employeeId=:employee and r.startDate<=:to and r.endDate>=:from and r.status in :statuses and r.id<>:exceptId")
    List<LeaveRequestEntity> candidates(@Param("employee") UUID employee, @Param("from") LocalDate from,
            @Param("to") LocalDate to, @Param("exceptId") UUID exceptId,
            @Param("statuses") List<RequestStatus> statuses);
    @Query("select r from LeaveRequestEntity r where r.employeeId=:employee and r.status=:status and r.startDate<=:date and r.endDate>=:date order by r.startTime asc, r.id asc")
    List<LeaveRequestEntity> approvedOn(@Param("employee") UUID employee,
            @Param("date") LocalDate date, @Param("status") RequestStatus status);
}

interface LeaveHistoryJpaRepository extends JpaRepository<LeaveRequestHistoryEntity, UUID>,
        JpaSpecificationExecutor<LeaveRequestHistoryEntity> { }

interface LeaveAttachmentJpaRepository extends JpaRepository<LeaveAttachmentEntity, UUID> {
    Page<LeaveAttachmentEntity> findByLeaveRequestId(UUID id, Pageable page);
    boolean existsByLeaveRequestId(UUID id);
}
