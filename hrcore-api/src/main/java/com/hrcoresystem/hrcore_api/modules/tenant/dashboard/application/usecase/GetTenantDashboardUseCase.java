package com.hrcoresystem.hrcore_api.modules.tenant.dashboard.application.usecase;

import com.hrcoresystem.hrcore_api.common.exception.BusinessException;
import com.hrcoresystem.hrcore_api.common.security.context.SecurityContextFacade;
import com.hrcoresystem.hrcore_api.common.tenancy.context.TenantContextHolder;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.model.TenantUserStatus;
import com.hrcoresystem.hrcore_api.modules.identity.users.domain.repository.TenantUserRepository;
import com.hrcoresystem.hrcore_api.modules.platform.tenantsettings.application.service.TenantTimezoneService;
import com.hrcoresystem.hrcore_api.modules.tenant.dashboard.application.dto.TenantDashboardResponse;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.dto.ReportFilter;
import com.hrcoresystem.hrcore_api.modules.tenant.reports.application.service.HrReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class GetTenantDashboardUseCase {

    private static final int MAX_RANGE_DAYS = 366;

    private final SecurityContextFacade security;
    private final TenantTimezoneService timezone;
    private final TenantUserRepository users;
    private final HrReportService reports;

    public GetTenantDashboardUseCase(
            SecurityContextFacade security,
            TenantTimezoneService timezone,
            TenantUserRepository users,
            HrReportService reports
    ) {
        this.security = security;
        this.timezone = timezone;
        this.users = users;
        this.reports = reports;
    }

    @Transactional(readOnly = true)
    public TenantDashboardResponse execute(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate to = requestedTo == null ? timezone.today() : requestedTo;
        LocalDate from = requestedFrom == null ? to.minusDays(30) : requestedFrom;
        validateRange(from, to);

        ReportFilter filter = new ReportFilter(from, to, null, null);
        List<TenantDashboardResponse.Metric> metrics = new ArrayList<>();
        List<TenantDashboardResponse.Section> sections = new ArrayList<>();

        if (has("users.list")) {
            addUsersSection(metrics, sections);
        }
        addReportSection(
                sections,
                metrics,
                "employees",
                "Empleados",
                "employees.list",
                () -> reports.employees(filter),
                List.of("totalEmployees", "activeEmployees", "on_leaveEmployees", "employeesWithoutCurrentAssignment")
        );
        addReportSection(
                sections,
                metrics,
                "contracts",
                "Contratos",
                "contracts.list",
                () -> reports.contracts(filter),
                List.of("totalContracts", "activeContracts", "pending_signatureContracts", "expiredContracts")
        );
        addReportSection(
                sections,
                metrics,
                "attendance",
                "Asistencia",
                "attendance.records.list",
                () -> reports.attendance(filter),
                List.of("totalRecords", "totalLateMinutes", "totalEarlyDepartureMinutes", "approvedOvertimeMinutes")
        );
        addReportSection(
                sections,
                metrics,
                "leaves",
                "Permisos",
                "leaves.requests.list",
                () -> reports.leaves(filter),
                List.of("totalRequests", "pendingDays", "approvedDays")
        );
        addReportSection(
                sections,
                metrics,
                "incidents",
                "Incidencias",
                "incidents.list",
                () -> reports.incidents(filter),
                List.of("totalIncidents")
        );

        return new TenantDashboardResponse(
                new TenantDashboardResponse.Metadata(
                        Instant.now(),
                        timezone.requiredZone().getId(),
                        TenantContextHolder.getRequired().tenantSlug(),
                        displayName(),
                        from,
                        to
                ),
                List.copyOf(metrics),
                List.copyOf(sections)
        );
    }

    private void addUsersSection(
            List<TenantDashboardResponse.Metric> metrics,
            List<TenantDashboardResponse.Section> sections
    ) {
        var tenantUsers = users.findAll();
        long active = tenantUsers.stream().filter(user -> user.status() == TenantUserStatus.ACTIVE).count();
        long pending = tenantUsers.stream().filter(user -> user.status() == TenantUserStatus.PENDING).count();
        long inactive = tenantUsers.stream().filter(user -> user.status() == TenantUserStatus.INACTIVE).count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", tenantUsers.size());
        data.put("active", active);
        data.put("pending", pending);
        data.put("inactive", inactive);
        sections.add(new TenantDashboardResponse.Section("users", "Usuarios", "users.list", data));
        metrics.add(metric("users.total", "Usuarios", tenantUsers.size(), "users.list"));
        metrics.add(metric("users.pending", "Usuarios pendientes", pending, "users.list"));
    }

    private void addReportSection(
            List<TenantDashboardResponse.Section> sections,
            List<TenantDashboardResponse.Metric> metrics,
            String key,
            String title,
            String permission,
            Supplier<Map<String, Object>> dataSupplier,
            List<String> metricKeys
    ) {
        if (!has(permission)) {
            return;
        }
        Map<String, Object> data = dataSupplier.get();
        sections.add(new TenantDashboardResponse.Section(
                key,
                title,
                permission,
                Collections.unmodifiableMap(new LinkedHashMap<>(data))
        ));
        metricKeys.stream()
                .filter(data::containsKey)
                .map(metricKey -> metric(key + "." + metricKey, label(metricKey), data.get(metricKey), permission))
                .forEach(metrics::add);
    }

    private TenantDashboardResponse.Metric metric(String key, String label, Object value, String permission) {
        return new TenantDashboardResponse.Metric(key, label, value, permission);
    }

    private boolean has(String permission) {
        return security.hasAuthority(permission);
    }

    private String displayName() {
        String value = security.getCurrentDisplayName();
        return value == null || value.isBlank() ? security.getCurrentEmail() : value;
    }

    private String label(String key) {
        return switch (key) {
            case "totalEmployees" -> "Empleados totales";
            case "activeEmployees" -> "Empleados activos";
            case "on_leaveEmployees" -> "Empleados con permiso";
            case "employeesWithoutCurrentAssignment" -> "Sin asignación vigente";
            case "totalContracts" -> "Contratos totales";
            case "activeContracts" -> "Contratos activos";
            case "pending_signatureContracts" -> "Contratos pendientes de firma";
            case "expiredContracts" -> "Contratos vencidos";
            case "totalRecords" -> "Registros de asistencia";
            case "totalLateMinutes" -> "Minutos de tardanza";
            case "totalEarlyDepartureMinutes" -> "Minutos de salida anticipada";
            case "approvedOvertimeMinutes" -> "Minutos extra aprobados";
            case "totalRequests" -> "Solicitudes de permiso";
            case "pendingDays" -> "Días pendientes";
            case "approvedDays" -> "Días aprobados";
            case "totalIncidents" -> "Incidencias";
            default -> key;
        };
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to) || from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new BusinessException("El rango del dashboard debe ser válido y no superar 366 días");
        }
    }
}
