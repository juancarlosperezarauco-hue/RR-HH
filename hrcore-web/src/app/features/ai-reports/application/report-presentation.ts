import { QueryResult, ReportColumn, ReportValue } from '../../../entities/reports/model/ai-conversation.model';

const metricLabels: Readonly<Record<string, string>> = {
  'employees.distinct_count': 'Empleados distintos',
  'attendance.record_count': 'Registros de asistencia',
  'attendance.late_days': 'Días de retraso',
  'attendance.late_minutes_total': 'Minutos de retraso'
};
const labels: Readonly<Record<string, string>> = {
  ...metricLabels, 'employee.id': 'ID del empleado', 'employee.full_name': 'Empleado',
  'employment_period.id': 'Período laboral', 'department.id': 'ID del departamento', 'department.name': 'Departamento',
  'position.id': 'ID del cargo', 'position.name': 'Cargo', 'attendance.work_date': 'Fecha de asistencia',
  'attendance.status': 'Estado de asistencia', 'attendance.first_in_local_time': 'Primera entrada (hora local)',
  'attendance.late_minutes': 'Minutos de retraso del registro', 'employee.employment_status_at_date': 'Estado laboral'
};
const numbers = new Intl.NumberFormat('es-BO');
const dates = new Intl.DateTimeFormat('es-BO', { timeZone: 'UTC', year: 'numeric', month: '2-digit', day: '2-digit' });
export const columnLabel = (key: string): string => labels[key] ?? key.replace(/[._]/g, ' ');
function dateValue(value: ReportValue | undefined): number | null {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const timestamp = Date.parse(`${value}T00:00:00Z`);
  return Number.isFinite(timestamp) && new Date(timestamp).toISOString().slice(0, 10) === value ? timestamp : null;
}
export function formatReportValue(value: ReportValue | undefined, column: ReportColumn): string {
  if (value === null || value === undefined) return '—';
  if (column.type === 'INTEGER' && typeof value === 'number' && Number.isFinite(value)) return numbers.format(value);
  if (column.type === 'DATE') { const date = dateValue(value); if (date !== null) return dates.format(date); }
  return String(value);
}
export interface ChartPoint { label: string; value: number; timestamp?: number; }
export interface ReportChart { kind: 'bar' | 'line' | null; metrics: string[]; points: ChartPoint[]; metric: string; reason: string; }

/** Charts display reported values only. They never aggregate or infer missing groups. */
export function reportChart(result: QueryResult, selectedMetric = ''): ReportChart {
  const unavailable = (reason: string): ReportChart => ({ kind: null, metrics: [], points: [], metric: '', reason });
  if (result.pageNumber !== 0 || result.hasMore) return unavailable('El reporte está paginado. Se muestra la tabla para evitar un gráfico de datos incompletos.');
  if (!result.rows.length) return unavailable('No hay resultados para representar.');
  const metrics = result.columns.filter(column => metricLabels[column.key] && column.type === 'INTEGER').map(column => column.key);
  if (!metrics.length) return unavailable('Este reporte de detalle no contiene métricas agregadas compatibles con gráficos.');
  const dimensions = result.columns.filter(column => !metrics.includes(column.key));
  let dimension: ReportColumn | undefined;
  if (dimensions.length === 1) dimension = dimensions[0];
  else if (dimensions.length === 2) {
    // A catalog identifier and its label describe one dimension, not two groups.
    for (const prefix of ['department', 'position', 'employee']) {
      const name = prefix === 'employee' ? 'full_name' : 'name';
      if (dimensions.some(column => column.key === `${prefix}.id`) && dimensions.some(column => column.key === `${prefix}.${name}`))
        dimension = dimensions.find(column => column.key === `${prefix}.${name}`);
    }
  }
  if (!dimension || !['TEXT', 'ENUM', 'DATE'].includes(dimension.type)) return unavailable('El gráfico requiere una sola categoría o fecha identificable. Este reporte se conserva como tabla.');
  const metric = metrics.includes(selectedMetric) ? selectedMetric : metrics[0];
  const points: ChartPoint[] = [];
  const categories = new Set<string>();
  for (const row of result.rows) {
    const category = row[dimension.key], value = row[metric];
    if (category === null || category === undefined || categories.has(String(category))) return unavailable('Hay categorías nulas o repetidas. No se agrupan ni se completan valores en el navegador.');
    if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) return unavailable('La métrica contiene valores nulos o no representables con precisión. Consulta la tabla.');
    categories.add(String(category));
    const point: ChartPoint = { label: formatReportValue(category, dimension), value };
    if (dimension.type === 'DATE') {
      const timestamp = dateValue(category); if (timestamp === null) return unavailable('Las fechas no permiten una tendencia temporal válida.');
      point.timestamp = timestamp;
    }
    points.push(point);
  }
  if (dimension.type === 'DATE') {
    if (points.length < 2) return unavailable('Se necesitan al menos dos fechas diferentes para mostrar una tendencia.');
    points.sort((a, b) => a.timestamp! - b.timestamp!);
    return { kind: 'line', metrics, metric, points, reason: '' };
  }
  if (points.length > 30) return unavailable('Hay más de 30 categorías; la tabla conserva mejor la legibilidad.');
  return { kind: 'bar', metrics, metric, points, reason: '' };
}
