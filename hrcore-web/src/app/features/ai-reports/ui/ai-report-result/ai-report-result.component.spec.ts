import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AiReportResultComponent } from './ai-report-result.component';
import { AiReportView, QueryResult } from '../../../../entities/reports/model/ai-conversation.model';

describe('AiReportResult: tabla, gráficos y actualización manual', () => {
  let fixture: ComponentFixture<AiReportResultComponent>;
  let result: QueryResult;
  const setResult = () => fixture.componentRef.setInput('view', { result, reportRunId: 'synthetic-run', executedAt: '2026-10-19T10:00:00Z' } satisfies AiReportView);
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AiReportResultComponent] }); fixture = TestBed.createComponent(AiReportResultComponent);
    result = { columns: [{ key: 'department.name', type: 'TEXT' }, { key: 'attendance.late_days', type: 'INTEGER' }], rows: [{ 'department.name': 'Ventas', 'attendance.late_days': 2 }, { 'department.name': 'Soporte', 'attendance.late_days': 4 }], pageNumber: 0, pageSize: 20, hasMore: false, warnings: [] };
    setResult(); fixture.detectChanges();
  });
  const button = (root: HTMLElement, text: string) => Array.from(root.querySelectorAll('button')).find(item => item.textContent?.trim() === text)!;
  it('muestra columnas dinámicas y tabla inicial; activar gráfico no ejecuta HTTP', () => {
    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('th')?.textContent).toContain('Departamento'); expect(root.querySelector('svg')).toBeNull();
    button(root, 'Barras').click(); fixture.detectChanges(); expect(root.querySelector('svg[role=img]')).not.toBeNull(); expect(root.querySelectorAll('rect').length).toBe(2);
    button(root, 'Tabla').click(); fixture.detectChanges(); expect(root.querySelector('table')).not.toBeNull();
  });
  it('refrescar y paginar emiten acciones explícitas sin inventar total de filas', () => {
    const refreshed = jasmine.createSpy(), paged = jasmine.createSpy(); fixture.componentInstance.refresh.subscribe(refreshed); fixture.componentInstance.pageChange.subscribe(paged);
    result = { ...result, hasMore: true }; setResult(); fixture.detectChanges(); const root: HTMLElement = fixture.nativeElement;
    button(root, 'Actualizar datos').click(); button(root, 'Página siguiente').click(); expect(refreshed).toHaveBeenCalledTimes(1); expect(paged).toHaveBeenCalledWith(1);
    expect(root.textContent).toContain('2 filas recibidas'); expect(root.textContent).toContain('pueden variar');
  });
  it('cambia a tabla con explicación si el refresh produce datos incompatibles', () => {
    const root: HTMLElement = fixture.nativeElement; button(root, 'Barras').click(); fixture.detectChanges();
    result = { ...result, hasMore: true }; setResult(); fixture.detectChanges(); expect(root.querySelector('svg')).toBeNull(); expect(root.textContent).toContain('datos incompletos');
  });
  it('muestra vacío, valores nulos y HTML como texto seguro', () => {
    result = { ...result, rows: [{ 'department.name': '<img onerror=alert(1)>', 'attendance.late_days': null }] }; setResult(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull(); expect(fixture.nativeElement.textContent).toContain('<img onerror=alert(1)>'); expect(fixture.nativeElement.textContent).toContain('—');
    result = { ...result, rows: [], warnings: ['MISSING_PRIMARY_ASSIGNMENT'] }; setResult(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('No hay resultados');
    expect(fixture.nativeElement.textContent).toContain('Hay empleados sin asignación principal vigente');
  });
  it('habilita líneas solo con fechas y métricas compatibles', () => {
    result = { ...result, columns: [{ key: 'attendance.work_date', type: 'DATE' }, { key: 'attendance.late_days', type: 'INTEGER' }], rows: [{ 'attendance.work_date': '2026-10-01', 'attendance.late_days': 2 }, { 'attendance.work_date': '2026-10-19', 'attendance.late_days': 4 }] };
    setResult(); fixture.detectChanges(); button(fixture.nativeElement, 'Líneas').click(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('polyline')).not.toBeNull(); expect(fixture.nativeElement.querySelectorAll('circle').length).toBe(2);
  });
  it('bloquea controles durante la consulta y al alcanzar la última página permitida', () => {
    fixture.componentRef.setInput('busy', true); fixture.detectChanges(); expect(button(fixture.nativeElement, 'Consultando…').disabled).toBeTrue();
    result = { ...result, pageNumber: 1000, hasMore: true }; setResult(); fixture.componentRef.setInput('busy', false); fixture.detectChanges(); expect(button(fixture.nativeElement, 'Página siguiente').disabled).toBeTrue();
  });
});
