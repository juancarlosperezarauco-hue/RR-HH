package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.ReportFilter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class HrReportService {
    private final JdbcTemplate jdbc;
    private final TenantTimezoneService timezone;
    private final SecurityContextFacade security;

    public HrReportService(JdbcTemplate jdbc, TenantTimezoneService timezone, SecurityContextFacade security) {
        this.jdbc = jdbc;
        this.timezone = timezone;
        this.security = security;
    }

    public Map<String, Object> employees(ReportFilter filter) {
        ReportFilter f = range(filter);
        Scope scope = employeeScope(f, "e", true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalEmployees", count("select count(distinct e.id) from employees e where 1=1" + scope.sql(), scope.args()));
        for (String status : List.of("ACTIVE", "INACTIVE", "TERMINATED", "ON_LEAVE", "SUSPENDED")) {
            result.put(camel(status) + "Employees", count(
                    "select count(distinct e.id) from employees e where e.employment_status=?" + scope.sql(),
                    prepend(status, scope.args())));
        }
        result.put("employeesWithoutCurrentAssignment", count(
                "select count(distinct e.id) from employees e where not exists(" +
                        "select 1 from employee_assignments x where x.employee_id=e.id and x.assignment_type='PRIMARY' " +
                        "and x.assignment_status='ACTIVE' and x.start_date<=? and (x.end_date is null or x.end_date>=?))" + scope.sql(),
                prependAll(List.of(f.toDate(), f.toDate()), scope.args())));
        result.put("byDepartment", grouped(
                "select d.name label,count(distinct e.id) total from employees e " +
                        "join employee_assignments x on x.employee_id=e.id and x.assignment_type='PRIMARY' and x.assignment_status='ACTIVE' " +
                        "join departments d on d.id=x.department_id where 1=1" + scope.sql() + " group by d.name order by d.name",
                scope.args()));
        result.put("byPosition", grouped(
                "select p.name label,count(distinct e.id) total from employees e " +
                        "join employee_assignments x on x.employee_id=e.id and x.assignment_type='PRIMARY' and x.assignment_status='ACTIVE' " +
                        "join positions p on p.id=x.position_id where 1=1" + scope.sql() + " group by p.name order by p.name",
                scope.args()));
        result.put("newEmployeesInPeriod", count(
                "select count(distinct e.id) from employees e where e.hire_date between ? and ?" + scope.sql(),
                prependAll(List.of(f.fromDate(), f.toDate()), scope.args())));
        result.put("terminationsInPeriod", count(
                "select count(distinct e.id) from employees e where e.termination_date between ? and ?" + scope.sql(),
                prependAll(List.of(f.fromDate(), f.toDate()), scope.args())));
        result.put("averageTenureMonths", scalar(
                "select round(coalesce(avg(greatest(0,? - e.hire_date)),0)/30.4375,1) from employees e where e.hire_date<=?" + scope.sql(),
                prependAll(List.of(f.toDate(), f.toDate()), scope.args())));
        result.put("byContractType", grouped(
                "select c.contract_type label,count(distinct c.employee_id) total from employment_contracts c join employees e on e.id=c.employee_id " +
                        "where c.start_date<=? and (c.end_date is null or c.end_date>=?)" + scope.sql() + " group by c.contract_type order by c.contract_type",
                prependAll(List.of(f.toDate(), f.toDate()), scope.args())));
        result.put("monthlyHires", grouped(
                "select to_char(date_trunc('month',e.hire_date),'YYYY-MM') label,count(*) total from employees e " +
                        "where e.hire_date between ? and ?" + scope.sql() + " group by 1 order by 1",
                prependAll(List.of(f.fromDate(), f.toDate()), scope.args())));
        long total = ((Number) result.get("totalEmployees")).longValue();
        long terminations = ((Number) result.get("terminationsInPeriod")).longValue();
        result.put("turnoverPercentage", percentage(terminations, total));
        return result;
    }

    public Map<String, Object> departments(ReportFilter filter) {
        ReportFilter f = range(filter);
        StringBuilder where = new StringBuilder(); List<Object> args = new ArrayList<>();
        addUuid(where, args, "d.id", f.departmentId()); addStatus(where, args, "d.status", f.status());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDepartments", count("select count(*) from departments d where 1=1" + where, args));
        result.put("activeDepartments", count("select count(*) from departments d where d.status='ACTIVE'" + withoutStatusSql(where, f.status()), withoutStatusArgs(args, f.status())));
        result.put("inactiveDepartments", count("select count(*) from departments d where d.status='INACTIVE'" + withoutStatusSql(where, f.status()), withoutStatusArgs(args, f.status())));
        List<Object> dated = prependAll(List.of(f.toDate(), f.toDate()), args);
        result.put("byEmployeeCount", grouped(
                "select d.name label,count(distinct x.employee_id) total from departments d left join employee_assignments x on x.department_id=d.id " +
                        "and x.assignment_type='PRIMARY' and x.assignment_status='ACTIVE' and x.start_date<=? and (x.end_date is null or x.end_date>=?) " +
                        "where 1=1" + where + " group by d.id,d.name order by total desc,d.name", dated));
        result.put("contractsByDepartment", grouped(
                "select d.name label,count(distinct c.id) total from departments d left join employee_assignments x on x.department_id=d.id " +
                        "left join employment_contracts c on c.employee_assignment_id=x.id and c.start_date<=? and (c.end_date is null or c.end_date>=?) " +
                        "where 1=1" + where + " group by d.id,d.name order by d.name", dated));
        result.put("absencesByDepartment", grouped(
                "select d.name label,count(distinct l.id) total from departments d left join employee_assignments x on x.department_id=d.id " +
                        "left join leave_requests l on l.employee_id=x.employee_id and l.status='APPROVED' and l.start_date<=? and l.end_date>=? " +
                        "where 1=1" + where + " group by d.id,d.name order by d.name",
                prependAll(List.of(f.toDate(), f.fromDate()), args)));
        return result;
    }

    public Map<String, Object> positions(ReportFilter filter) {
        ReportFilter f = range(filter);
        StringBuilder where = new StringBuilder(); List<Object> args = new ArrayList<>();
        addUuid(where, args, "p.id", f.positionId()); addStatus(where, args, "p.status", f.status());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPositions", count("select count(*) from positions p where 1=1" + where, args));
        result.put("activePositions", count("select count(*) from positions p where p.status='ACTIVE'" + withoutStatusSql(where, f.status()), withoutStatusArgs(args, f.status())));
        result.put("inactivePositions", count("select count(*) from positions p where p.status='INACTIVE'" + withoutStatusSql(where, f.status()), withoutStatusArgs(args, f.status())));
        List<Object> dated = prependAll(List.of(f.toDate(), f.toDate()), args);
        result.put("byEmployeeCount", grouped(
                "select p.name label,count(distinct x.employee_id) total from positions p left join employee_assignments x on x.position_id=p.id " +
                        "and x.assignment_type='PRIMARY' and x.assignment_status='ACTIVE' and x.start_date<=? and (x.end_date is null or x.end_date>=?) " +
                        "where 1=1" + where + " group by p.id,p.name order by total desc,p.name", dated));
        result.put("positionsWithoutEmployees", count(
                "select count(*) from positions p where not exists(select 1 from employee_assignments x where x.position_id=p.id and x.assignment_type='PRIMARY' " +
                        "and x.assignment_status='ACTIVE' and x.start_date<=? and (x.end_date is null or x.end_date>=?))" + where, dated));
        result.put("averageSalaryByPosition", grouped(
                "select p.name label,round(coalesce(avg(c.base_salary),0),2) total from positions p left join employee_assignments x on x.position_id=p.id " +
                        "left join employment_contracts c on c.employee_assignment_id=x.id and c.status='ACTIVE' where 1=1" + where +
                        " group by p.id,p.name order by p.name", args));
        return result;
    }

    public Map<String, Object> contracts(ReportFilter f) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        addUuid(where, args, "c.employee_id", f.employeeId());
        addUuid(where, args, "c.employment_period_id", f.employmentPeriodId());
        if (f.departmentId() != null) {
            where.append(" and exists(select 1 from employee_assignments x where x.employee_id=c.employee_id and x.department_id=?)");
            args.add(f.departmentId());
        }
        if (f.positionId() != null) {
            where.append(" and exists(select 1 from employee_assignments x where x.employee_id=c.employee_id and x.position_id=?)");
            args.add(f.positionId());
        }
        addStatus(where, args, "c.status", f.status());
        if (f.fromDate() != null) {
            where.append(" and (c.end_date is null or c.end_date>=?)");
            args.add(f.fromDate());
        }
        if (f.toDate() != null) {
            where.append(" and c.start_date<=?");
            args.add(f.toDate());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalContracts", count("select count(*) from employment_contracts c where 1=1" + where, args));
        for (String status : List.of("ACTIVE", "DRAFT", "PENDING_SIGNATURE", "EXPIRED", "TERMINATED", "CANCELLED")) {
            result.put(camel(status) + "Contracts", count(
                    "select count(*) from employment_contracts c where c.status=?" + where,
                    prepend(status, args)));
        }
        result.put("byStatus", grouped("select c.status label,count(*) total from employment_contracts c where 1=1" + where + " group by c.status order by c.status", args));
        result.put("renewedContracts", count("select count(*) from employment_contracts c where c.renewed_from_contract_id is not null" + where, args));
        result.put("expiringNext30Days", count(
                "select count(*) from employment_contracts c where c.status='ACTIVE' and c.end_date between current_date and current_date+30" + where, args));
        result.put("averageSalary", scalar("select round(coalesce(avg(c.base_salary),0),2) from employment_contracts c where 1=1" + where, args));
        result.put("averageDurationDays", scalar(
                "select round(coalesce(avg(coalesce(c.end_date,c.termination_date,current_date)-c.start_date),0),1) from employment_contracts c where 1=1" + where, args));
        result.put("byType", grouped("select c.contract_type label,count(*) total from employment_contracts c where 1=1" + where + " group by c.contract_type order by c.contract_type", args));
        result.put("salaryByDepartment", grouped(
                "select d.name label,round(avg(c.base_salary),2) total from employment_contracts c join employee_assignments x on x.id=c.employee_assignment_id " +
                        "join departments d on d.id=x.department_id where 1=1" + where + " group by d.name order by d.name", args));
        result.put("salaryByPosition", grouped(
                "select p.name label,round(avg(c.base_salary),2) total from employment_contracts c join employee_assignments x on x.id=c.employee_assignment_id " +
                        "join positions p on p.id=x.position_id where 1=1" + where + " group by p.name order by p.name", args));
        return result;
    }

    public Map<String, Object> attendance(ReportFilter filter) {
        ReportFilter f = range(filter);
        Scope scope = recordScope(f, "r");
        List<Object> args = prependAll(List.of(f.fromDate(), f.toDate()), scope.args());
        String where = " where r.work_date between ? and ?" + scope.sql();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromDate", f.fromDate()); result.put("toDate", f.toDate());
        result.put("totalRecords", count("select count(*) from attendance_records r" + where, args));
        result.put("totalWorkedMinutes", scalar("select coalesce(sum(r.worked_minutes),0) from attendance_records r" + where, args));
        result.put("totalLateMinutes", scalar("select coalesce(sum(r.late_minutes),0) from attendance_records r" + where, args));
        result.put("totalEarlyDepartureMinutes", scalar("select coalesce(sum(r.early_departure_minutes),0) from attendance_records r" + where, args));
        result.put("byStatus", grouped("select r.status label,count(*) total from attendance_records r" + where + " group by r.status order by r.status", args));
        result.put("approvedOvertimeMinutes", scalar(
                "select coalesce(sum(o.approved_minutes),0) from attendance_overtime o join attendance_records r on r.id=o.attendance_record_id" + where + " and o.status='APPROVED'",
                args));
        long records = ((Number) result.get("totalRecords")).longValue();
        long present = count("select count(*) from attendance_records r" + where + " and r.status in ('PRESENT','LATE')", args);
        long late = count("select count(*) from attendance_records r" + where + " and r.status='LATE'", args);
        result.put("presentRecords", present);
        result.put("absentRecords", count("select count(*) from attendance_records r" + where + " and r.status='ABSENT'", args));
        result.put("lateRecords", late);
        result.put("attendancePercentage", percentage(present, records));
        result.put("punctualityPercentage", percentage(Math.max(0, present-late), present));
        result.put("averageWorkedHours", scalar("select round(coalesce(avg(r.worked_minutes),0)/60.0,2) from attendance_records r" + where, args));
        result.put("byDepartment", grouped(
                "select d.name label,count(*) filter(where r.status in ('PRESENT','LATE')) total from attendance_records r " +
                        "join employee_assignments x on x.employee_id=r.employee_id and x.assignment_type='PRIMARY' and x.start_date<=r.work_date and (x.end_date is null or x.end_date>=r.work_date) " +
                        "join departments d on d.id=x.department_id" + where + " group by d.name order by d.name", args));
        result.put("dailyTrend", grouped(
                "select to_char(r.work_date,'YYYY-MM-DD') label,count(*) filter(where r.status in ('PRESENT','LATE')) total from attendance_records r" + where + " group by r.work_date order by r.work_date", args));
        return result;
    }

    public Map<String, Object> leaves(ReportFilter filter) {
        ReportFilter f = range(filter);
        Scope scope = recordScope(f, "r");
        List<Object> args = prependAll(List.of(f.toDate(), f.fromDate()), scope.args());
        String where = " where r.start_date<=? and r.end_date>=?" + scope.sql();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromDate", f.fromDate()); result.put("toDate", f.toDate());
        result.put("totalRequests", count("select count(distinct r.id) from leave_requests r" + where, args));
        result.put("approvedDays", scalar("select coalesce(sum(r.requested_days),0) from leave_requests r" + where + " and r.status='APPROVED'", args));
        result.put("pendingDays", scalar("select coalesce(sum(r.requested_days),0) from leave_requests r" + where + " and r.status='PENDING'", args));
        result.put("byStatus", grouped("select r.status label,count(distinct r.id) total from leave_requests r" + where + " group by r.status order by r.status", args));
        long requests = ((Number) result.get("totalRequests")).longValue();
        long approved = count("select count(*) from leave_requests r" + where + " and r.status='APPROVED'", args);
        result.put("approvedRequests", approved);
        result.put("rejectedRequests", count("select count(*) from leave_requests r" + where + " and r.status='REJECTED'", args));
        result.put("pendingRequests", count("select count(*) from leave_requests r" + where + " and r.status='PENDING'", args));
        result.put("approvalPercentage", percentage(approved, requests));
        result.put("totalApprovedDays", scalar("select coalesce(sum(r.requested_days),0) from leave_requests r" + where + " and r.status='APPROVED'", args));
        result.put("byType", grouped("select t.name label,count(*) total from leave_requests r join leave_types t on t.id=r.leave_type_id" + where + " group by t.name order by t.name", args));
        result.put("byDepartment", grouped(
                "select d.name label,count(distinct r.id) total from leave_requests r join employee_assignments x on x.employee_id=r.employee_id " +
                        "and x.assignment_type='PRIMARY' and x.start_date<=r.start_date and (x.end_date is null or x.end_date>=r.start_date) " +
                        "join departments d on d.id=x.department_id" + where + " group by d.name order by d.name", args));
        result.put("byPayment", grouped(
                "select case when t.paid then 'REMUNERADA' else 'NO_REMUNERADA' end label,count(*) total from leave_requests r " +
                        "join leave_types t on t.id=r.leave_type_id" + where + " group by t.paid order by t.paid desc", args));
        return result;
    }

    public Map<String, Object> incidents(ReportFilter filter) {
        ReportFilter f = range(filter);
        ZoneId zone = timezone.requiredZone();
        Scope scope = recordScope(f, "i");
        List<Object> args = prependAll(List.of(
                Timestamp.from(f.fromDate().atStartOfDay(zone).toInstant()),
                Timestamp.from(f.toDate().plusDays(1).atStartOfDay(zone).toInstant())), scope.args());
        String where = " where i.occurred_at>=? and i.occurred_at<?" + scope.sql();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromDate", f.fromDate()); result.put("toDate", f.toDate());
        result.put("totalIncidents", count("select count(distinct i.id) from employee_incidents i" + where, args));
        result.put("byStatus", grouped("select i.status label,count(distinct i.id) total from employee_incidents i" + where + " group by i.status order by i.status", args));
        result.put("byType", grouped("select i.incident_type label,count(distinct i.id) total from employee_incidents i" + where + " group by i.incident_type order by i.incident_type", args));
        result.put("bySeverity", grouped("select i.severity label,count(distinct i.id) total from employee_incidents i" + where + " group by i.severity order by i.severity", args));
        return result;
    }

    public Map<String, Object> files(ReportFilter f) {
        Scope scope = employeeScope(f, "e", false);
        List<Object> args = new ArrayList<>(scope.args());
        String statusSql = "";
        if (hasText(f.status())) { statusSql = " and ef.status=?"; args.add(normalizeStatus(f.status())); }
        String where = " where 1=1" + scope.sql() + statusSql;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFiles", count("select count(*) from employee_files ef join employees e on e.id=ef.employee_id" + where, args));
        result.put("activeFiles", count("select count(*) from employee_files ef join employees e on e.id=ef.employee_id where ef.status='ACTIVE'" + scope.sql(), scope.args()));
        result.put("archivedFiles", count("select count(*) from employee_files ef join employees e on e.id=ef.employee_id where ef.status='ARCHIVED'" + scope.sql(), scope.args()));
        result.put("totalDocuments", count("select count(*) from employee_documents d join employee_files ef on ef.id=d.employee_file_id join employees e on e.id=ef.employee_id" + where, args));
        result.put("byStatus", grouped("select ef.status label,count(*) total from employee_files ef join employees e on e.id=ef.employee_id" + where + " group by ef.status order by ef.status", args));
        result.put("employeesWithoutFile", count(
                "select count(*) from employees e where not exists(select 1 from employee_files ef where ef.employee_id=e.id)" + scope.sql(), scope.args()));
        result.put("pendingDocuments", count(
                "select count(*) from employee_documents d join employee_files ef on ef.id=d.employee_file_id join employees e on e.id=ef.employee_id " +
                        "where d.verification_status='PENDING'" + scope.sql(), scope.args()));
        result.put("expiredDocuments", count(
                "select count(*) from employee_documents d join employee_files ef on ef.id=d.employee_file_id join employees e on e.id=ef.employee_id " +
                        "where d.expires_at<current_date and d.status='ACTIVE'" + scope.sql(), scope.args()));
        result.put("documentsExpiringNext30Days", count(
                "select count(*) from employee_documents d join employee_files ef on ef.id=d.employee_file_id join employees e on e.id=ef.employee_id " +
                        "where d.expires_at between current_date and current_date+30 and d.status='ACTIVE'" + scope.sql(), scope.args()));
        result.put("byDocumentType", grouped(
                "select d.document_type label,count(*) total from employee_documents d join employee_files ef on ef.id=d.employee_file_id " +
                        "join employees e on e.id=ef.employee_id where 1=1" + scope.sql() + " group by d.document_type order by d.document_type", scope.args()));
        long totalFiles = ((Number) result.get("totalFiles")).longValue();
        long completeFiles = count(
                "select count(*) from employee_files ef join employees e on e.id=ef.employee_id where " +
                        "exists(select 1 from employee_documents d where d.employee_file_id=ef.id and d.document_type='IDENTITY' and d.verification_status='VERIFIED' and d.status='ACTIVE') " +
                        "and exists(select 1 from employee_documents d where d.employee_file_id=ef.id and d.document_type='CONTRACT' and d.verification_status='VERIFIED' and d.status='ACTIVE')" + scope.sql(), scope.args());
        result.put("completeFiles", completeFiles);
        result.put("incompleteFiles", Math.max(0, totalFiles-completeFiles));
        result.put("completenessPercentage", percentage(completeFiles, totalFiles));
        return result;
    }

    public Map<String, Object> shifts(ReportFilter f) {
        List<Object> shiftArgs = new ArrayList<>();
        StringBuilder shiftWhere = new StringBuilder();
        addStatus(shiftWhere, shiftArgs, "s.status", f.status());
        boolean scoped = f.employeeId() != null || f.employmentPeriodId() != null || f.departmentId() != null || f.positionId() != null;
        Scope employee = employeeScope(f, "e", false);
        if (scoped) {
            shiftWhere.append(" and exists(select 1 from employee_shift_assignments sa join employees e on e.id=sa.employee_id where sa.shift_id=s.id")
                    .append(employee.sql()).append(')');
            shiftArgs.addAll(employee.args());
        }
        String unfilteredScope = scoped ? shiftWhere.toString().replace(" and s.status=?", "") : "";
        List<Object> unfilteredArgs = scoped ? withoutFirstStatusArg(shiftArgs, f.status()) : List.of();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalShifts", count("select count(*) from work_shifts s where 1=1" + shiftWhere, shiftArgs));
        result.put("activeShifts", count("select count(*) from work_shifts s where s.status='ACTIVE'" + unfilteredScope, unfilteredArgs));
        result.put("inactiveShifts", count("select count(*) from work_shifts s where s.status='INACTIVE'" + unfilteredScope, unfilteredArgs));
        result.put("totalAssignments", count("select count(*) from employee_shift_assignments sa join employees e on e.id=sa.employee_id where 1=1" + employee.sql(), employee.args()));
        result.put("activeAssignments", count("select count(*) from employee_shift_assignments sa join employees e on e.id=sa.employee_id where sa.status='ACTIVE'" + employee.sql(), employee.args()));
        result.put("byStatus", grouped("select s.status label,count(*) total from work_shifts s where 1=1" + shiftWhere + " group by s.status order by s.status", shiftArgs));
        result.put("employeesWithShift", count(
                "select count(distinct e.id) from employees e join employee_shift_assignments sa on sa.employee_id=e.id and sa.status='ACTIVE' " +
                        "and sa.start_date<=current_date and (sa.end_date is null or sa.end_date>=current_date) where 1=1" + employee.sql(), employee.args()));
        result.put("employeesWithoutShift", count(
                "select count(*) from employees e where not exists(select 1 from employee_shift_assignments sa where sa.employee_id=e.id and sa.status='ACTIVE' " +
                        "and sa.start_date<=current_date and (sa.end_date is null or sa.end_date>=current_date))" + employee.sql(), employee.args()));
        result.put("byShift", grouped(
                "select s.name label,count(distinct sa.employee_id) total from work_shifts s left join employee_shift_assignments sa on sa.shift_id=s.id and sa.status='ACTIVE' " +
                        "where 1=1" + shiftWhere + " group by s.id,s.name order by total desc,s.name", shiftArgs));
        result.put("byDayPeriod", grouped(
                "select case when s.crosses_midnight or s.start_time>=time '18:00' then 'NOCTURNO' else 'DIURNO' end label,count(*) total " +
                        "from work_shifts s where 1=1" + shiftWhere + " group by 1 order by 1", shiftArgs));
        return result;
    }

    public Map<String, Object> integrated(ReportFilter filter) {
        ReportFilter f = range(filter);
        Scope scope = employeeScope(f, "e", false);
        List<Object> todayArgs = prependAll(List.of(f.toDate(), f.toDate()), scope.args());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeesWithActiveContractAndShift", count(
                "select count(distinct e.id) from employees e where exists(select 1 from employment_contracts c where c.employee_id=e.id and c.status='ACTIVE' and c.start_date<=? and (c.end_date is null or c.end_date>=?)) " +
                        "and exists(select 1 from employee_shift_assignments sa where sa.employee_id=e.id and sa.status='ACTIVE' and sa.start_date<=? and (sa.end_date is null or sa.end_date>=?))" + scope.sql(),
                prependAll(List.of(f.toDate(), f.toDate(), f.toDate(), f.toDate()), scope.args())));
        result.put("employeesWithoutActiveContract", count(
                "select count(*) from employees e where not exists(select 1 from employment_contracts c where c.employee_id=e.id and c.status='ACTIVE' and c.start_date<=? and (c.end_date is null or c.end_date>=?))" + scope.sql(), todayArgs));
        result.put("employeesWithoutAssignment", count(
                "select count(*) from employees e where not exists(select 1 from employee_assignments x where x.employee_id=e.id and x.assignment_type='PRIMARY' and x.assignment_status='ACTIVE' and x.start_date<=? and (x.end_date is null or x.end_date>=?))" + scope.sql(), todayArgs));
        result.put("employeesWithoutShift", count(
                "select count(*) from employees e where not exists(select 1 from employee_shift_assignments sa where sa.employee_id=e.id and sa.status='ACTIVE' and sa.start_date<=? and (sa.end_date is null or sa.end_date>=?))" + scope.sql(), todayArgs));
        result.put("employeesWithoutFile", count("select count(*) from employees e where not exists(select 1 from employee_files ef where ef.employee_id=e.id)" + scope.sql(), scope.args()));
        result.put("contractsExpiringNext30Days", count(
                "select count(*) from employment_contracts c join employees e on e.id=c.employee_id where c.status='ACTIVE' and c.end_date between ? and ?" + scope.sql(),
                prependAll(List.of(f.toDate(), f.toDate().plusDays(30)), scope.args())));
        result.put("attendanceByDepartment", attendance(f).get("byDepartment"));
        result.put("absencesByDepartment", leaves(f).get("byDepartment"));
        return result;
    }

    public byte[] csv(String report, ReportFilter filter) {
        Map<String, Object> data = data(report, filter);
        StringBuilder csv = new StringBuilder("indicador,valor\n");
        data.forEach((key, value) -> csv.append(cell(key)).append(',').append(cell(String.valueOf(value))).append('\n'));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Object> data(String report, ReportFilter filter) {
        return switch (report) {
            case "employees" -> employees(filter); case "departments" -> departments(filter); case "positions" -> positions(filter); case "contracts" -> contracts(filter);
            case "attendance" -> attendance(filter); case "leaves" -> leaves(filter);
            case "incidents" -> incidents(filter); case "files" -> files(filter); case "shifts" -> shifts(filter); case "integrated" -> integrated(filter);
            default -> throw new BusinessException("Reporte no soportado");
        };
    }

    public void assertExportAllowed(String report) {
        String permission = switch (report) {
            case "employees" -> "hr.reports.employees.read"; case "departments" -> "hr.reports.departments.read"; case "positions" -> "hr.reports.positions.read"; case "contracts" -> "hr.reports.contracts.read";
            case "attendance" -> "hr.reports.attendance.read"; case "leaves" -> "hr.reports.leaves.read";
            case "incidents" -> "hr.reports.incidents.read"; case "files" -> "hr.reports.files.read";
            case "shifts" -> "hr.reports.shifts.read"; case "integrated" -> "hr.reports.integrated.read"; default -> throw new BusinessException("Reporte no soportado");
        };
        if (!security.hasAuthority(permission)) throw new AccessDeniedException("No tienes permiso para exportar este reporte");
    }

    private ReportFilter range(ReportFilter f) {
        LocalDate to = f.toDate() == null ? timezone.today() : f.toDate();
        LocalDate from = f.fromDate() == null ? to.minusDays(30) : f.fromDate();
        if (from.isAfter(to) || from.plusDays(366).isBefore(to)) throw new BusinessException("El rango debe ser válido y no superar 366 días");
        return new ReportFilter(from, to, f.departmentId(), f.positionId(), f.employeeId(), f.employmentPeriodId(), f.status());
    }

    private Scope employeeScope(ReportFilter f, String alias, boolean includeStatus) {
        StringBuilder sql = new StringBuilder(); List<Object> args = new ArrayList<>();
        addUuid(sql, args, alias + ".id", f.employeeId());
        if (f.employmentPeriodId() != null) { sql.append(" and exists(select 1 from employment_periods ep where ep.id=? and ep.employee_id=").append(alias).append(".id)"); args.add(f.employmentPeriodId()); }
        if (f.departmentId() != null) { sql.append(" and exists(select 1 from employee_assignments ax where ax.employee_id=").append(alias).append(".id and ax.department_id=?)"); args.add(f.departmentId()); }
        if (f.positionId() != null) { sql.append(" and exists(select 1 from employee_assignments ax where ax.employee_id=").append(alias).append(".id and ax.position_id=?)"); args.add(f.positionId()); }
        if (includeStatus) addStatus(sql, args, alias + ".employment_status", f.status());
        return new Scope(sql.toString(), args);
    }

    private Scope recordScope(ReportFilter f, String alias) {
        StringBuilder sql = new StringBuilder(); List<Object> args = new ArrayList<>();
        addUuid(sql, args, alias + ".employee_id", f.employeeId()); addUuid(sql, args, alias + ".employment_period_id", f.employmentPeriodId());
        if (f.departmentId() != null) { sql.append(" and exists(select 1 from employee_assignments ax where ax.employee_id=").append(alias).append(".employee_id and ax.department_id=?)"); args.add(f.departmentId()); }
        if (f.positionId() != null) { sql.append(" and exists(select 1 from employee_assignments ax where ax.employee_id=").append(alias).append(".employee_id and ax.position_id=?)"); args.add(f.positionId()); }
        addStatus(sql, args, alias + ".status", f.status());
        return new Scope(sql.toString(), args);
    }

    private void addUuid(StringBuilder sql, List<Object> args, String column, UUID value) { if (value != null) { sql.append(" and ").append(column).append("=?"); args.add(value); } }
    private void addStatus(StringBuilder sql, List<Object> args, String column, String status) { if (hasText(status)) { sql.append(" and ").append(column).append("=?"); args.add(normalizeStatus(status)); } }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String normalizeStatus(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private String camel(String value) { String[] parts=value.toLowerCase(Locale.ROOT).split("_"); StringBuilder result=new StringBuilder(parts[0]); for(int i=1;i<parts.length;i++) result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1)); return result.toString(); }
    private long count(String sql, List<?> args) { Long value=jdbc.queryForObject(sql,Long.class,args.toArray()); return value==null?0:value; }
    private Object scalar(String sql, List<?> args) { return jdbc.queryForObject(sql,Object.class,args.toArray()); }
    private List<Map<String,Object>> grouped(String sql,List<?> args){return jdbc.queryForList(sql,args.toArray());}
    private List<Object> prepend(Object value,List<Object> args){List<Object> result=new ArrayList<>();result.add(value);result.addAll(args);return result;}
    private List<Object> prependAll(List<?> values,List<Object> args){List<Object> result=new ArrayList<>(values);result.addAll(args);return result;}
    private List<Object> withoutFirstStatusArg(List<Object> args,String status){if(!hasText(status)||args.isEmpty())return new ArrayList<>(args);return new ArrayList<>(args.subList(1,args.size()));}
    private String withoutStatusSql(StringBuilder sql,String status){return hasText(status)?sql.toString().replaceFirst(" and [a-z]+\\.status=\\?",""):sql.toString();}
    private List<Object> withoutStatusArgs(List<Object> args,String status){if(!hasText(status)||args.isEmpty())return new ArrayList<>(args);return new ArrayList<>(args.subList(0,args.size()-1));}
    private double percentage(long part,long total){return total==0?0.0:Math.round((part*10000.0/total))/100.0;}
    private String cell(String value){if(!value.isEmpty()&&"=+-@".indexOf(value.charAt(0))>=0)value="'"+value;return"\""+value.replace("\"","\"\"")+"\"";}
    private record Scope(String sql,List<Object> args){}
}
