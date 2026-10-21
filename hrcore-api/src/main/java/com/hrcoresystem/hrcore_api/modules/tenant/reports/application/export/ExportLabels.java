package com.hrcoresystem.hrcore_api.modules.tenant.reports.application.export;

import java.util.Map;

final class ExportLabels {
    private ExportLabels() {}
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("employee.id", "ID de empleado"), Map.entry("employee.full_name", "Nombre completo"),
            Map.entry("employment_period.id", "ID de período laboral"),
            Map.entry("department.id", "ID de departamento"), Map.entry("department.name", "Departamento"),
            Map.entry("position.id", "ID de cargo"), Map.entry("position.name", "Cargo"),
            Map.entry("attendance.work_date", "Fecha"), Map.entry("attendance.status", "Estado"),
            Map.entry("attendance.first_in_local_time", "Primera entrada local"),
            Map.entry("attendance.late_minutes", "Minutos de retraso"),
            Map.entry("employees.distinct_count", "Empleados"),
            Map.entry("attendance.record_count", "Registros de asistencia"),
            Map.entry("attendance.late_days", "Días con retraso"),
            Map.entry("attendance.late_minutes_total", "Minutos totales de retraso"));
    static String label(String key) { return LABELS.getOrDefault(key, key); }
}
