import { QueryResult } from '../../../entities/reports/model/ai-conversation.model';
import { columnLabel, formatReportValue, reportChart } from './report-presentation';

const aggregate = (): QueryResult => ({ columns: [{ key: 'department.name', type: 'TEXT' }, { key: 'attendance.late_days', type: 'INTEGER' }, { key: 'attendance.late_minutes_total', type: 'INTEGER' }], rows: [
  { 'department.name': 'Ventas', 'attendance.late_days': 3, 'attendance.late_minutes_total': 35 },
  { 'department.name': 'Soporte', 'attendance.late_days': 1, 'attendance.late_minutes_total': 5 }
], pageNumber: 0, pageSize: 20, hasMore: false, warnings: [] });

describe('Presentación de reportes: formato y gráficos sin inferencias', () => {
  it('formatea fechas DATE sin cambio de día y conserva horas locales y nulos', () => {
    expect(formatReportValue('2026-10-19', { key: 'date', type: 'DATE' })).toBe(new Intl.DateTimeFormat('es-BO', { timeZone: 'UTC', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date('2026-10-19T00:00:00Z')));
    expect(formatReportValue('08:30:00', { key: 'time', type: 'LOCAL_TIME' })).toBe('08:30:00');
    expect(formatReportValue(null, { key: 'x', type: 'TEXT' })).toBe('—');
    expect(formatReportValue(12500, { key: 'x', type: 'INTEGER' })).toBe(new Intl.NumberFormat('es-BO').format(12500));
    expect(columnLabel('attendance.late_days')).toBe('Días de retraso');
  });
  it('conserva los valores exactos de barras y permite seleccionar métricas sin sumarlas', () => {
    const result = aggregate(); const chart = reportChart(result, 'attendance.late_minutes_total');
    expect(chart.kind).toBe('bar'); expect(chart.metrics.length).toBe(2);
    expect(chart.points.map(point => point.value)).toEqual([35, 5]); expect(result.rows.length).toBe(2);
  });
  it('rechaza páginas incompletas aunque la última página diga hasMore=false', () => {
    const result = aggregate(); result.hasMore = true; expect(reportChart(result).kind).toBeNull();
    result.hasMore = false; result.pageNumber = 1; expect(reportChart(result).kind).toBeNull();
  });
  it('no agrupa categorías repetidas ni convierte nulos a cero', () => {
    const result = aggregate(); result.rows[1]['department.name'] = 'Ventas'; expect(reportChart(result).kind).toBeNull();
    result.rows[1]['department.name'] = null; expect(reportChart(result).kind).toBeNull();
    result.rows[1]['department.name'] = 'Soporte'; result.rows[0]['attendance.late_days'] = null; expect(reportChart(result).kind).toBeNull();
  });
  it('no representa números no finitos, negativos, inseguros ni cadenas como métricas numéricas', () => {
    for (const value of [Infinity, NaN, -1, Number.MAX_SAFE_INTEGER + 1, '3']) {
      const result = aggregate(); result.rows[0]['attendance.late_days'] = value; expect(reportChart(result).kind).toBeNull();
    }
  });
  it('rechaza detalles numéricos, dimensiones múltiples y exceso de categorías', () => {
    const result = aggregate(); result.columns.push({ key: 'position.name', type: 'TEXT' }); expect(reportChart(result).kind).toBeNull();
    const detail = aggregate(); detail.columns[1].key = 'attendance.late_minutes'; detail.columns.pop(); expect(reportChart(detail).kind).toBeNull();
    const large = aggregate(); large.rows = Array.from({ length: 31 }, (_, index) => ({ 'department.name': `Ficticio ${index}`, 'attendance.late_days': index })); expect(reportChart(large).kind).toBeNull();
  });
  it('ordena puntos temporales sin rellenar fechas ni mutar las filas del backend', () => {
    const result = aggregate(); result.columns[0] = { key: 'attendance.work_date', type: 'DATE' };
    result.rows[0]['attendance.work_date'] = '2026-10-19'; result.rows[1]['attendance.work_date'] = '2026-10-01';
    const chart = reportChart(result); expect(chart.kind).toBe('line'); expect(chart.points.length).toBe(2);
    expect(chart.points[0].timestamp).toBe(Date.parse('2026-10-01T00:00:00Z'));
    expect(result.rows[0]['attendance.work_date']).toBe('2026-10-19');
    result.rows[1]['attendance.work_date'] = '2026-02-30'; expect(reportChart(result).kind).toBeNull();
  });
  it('permite ID y etiqueta de la misma dimensión pero no mezcla dos dimensiones', () => {
    const result = aggregate(); result.columns.unshift({ key: 'department.id', type: 'UUID' }); expect(reportChart(result).kind).toBe('bar');
    result.columns[0] = { key: 'employee.id', type: 'UUID' }; expect(reportChart(result).kind).toBeNull();
  });
});
