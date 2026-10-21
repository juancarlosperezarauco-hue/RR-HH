import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HrReportsPageComponent } from './hr-reports-page.component';
import { ReportsService } from '../../entities/reports';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { AiConversationPanelComponent } from '../../features/ai-reports/ui/ai-conversation-panel/ai-conversation-panel.component';

@Component({ selector: 'app-ai-conversation-panel', standalone: true, template: 'Asistente ficticio' })
class AssistantStub { static created = 0; constructor() { AssistantStub.created++; } }

describe('HrReportsPage: regresión de reportes operativos', () => {
  let service: jasmine.SpyObj<ReportsService>;
  beforeEach(async () => {
    AssistantStub.created = 0;
    service = jasmine.createSpyObj('ReportsService', ['get', 'export']);
    service.get.and.returnValue(of({ success: true, message: 'OK', timestamp: '', data: {
      totalEmployees: 51, activeEmployees: 50, totalContracts: 7, totalRecords: 12, totalRequests: 4, totalIncidents: 2,
      byDepartment: { Ventas: 10 }, byPosition: { Analista: 5 }, byStatus: { ACTIVE: 1 }, byType: { OTHER: 2 }, bySeverity: { LOW: 2 }
    } }));
    await TestBed.configureTestingModule({ imports: [HrReportsPageComponent], providers: [
      { provide: ReportsService, useValue: service }, { provide: ToastService, useValue: { error: jasmine.createSpy() } }
    ] }).overrideComponent(HrReportsPageComponent, { remove: { imports: [AiConversationPanelComponent] }, add: { imports: [AssistantStub] } }).compileComponents();
  });
  it('conserva los cinco reportes y sus indicadores, filtros y actualización', () => {
    const fixture = TestBed.createComponent(HrReportsPageComponent); fixture.detectChanges(); const page = fixture.componentInstance;
    expect(service.get).toHaveBeenCalledOnceWith('employees', undefined, undefined);
    page.fromDate = '2026-01-01'; page.toDate = '2026-02-01';
    const expected = { employees: '51', contracts: '7', attendance: '12', leaves: '4', incidents: '2' };
    for (const report of page.reportTypes) {
      page.select(report.id); fixture.detectChanges();
      expect(service.get).toHaveBeenCalledWith(report.id, '2026-01-01', '2026-02-01');
      expect(page.metrics()[0].value).toBe(expected[report.id]); expect(page.groups().length).toBeGreaterThan(0);
    }
    page.load(); expect(service.get.calls.count()).toBe(7);
  });
  it('mantiene CSV para los cinco tipos y los filtros del reporte seleccionado', () => {
    service.export.and.returnValue(of(new Blob(['synthetic'], { type: 'text/csv' })));
    const createUrl = spyOn(URL, 'createObjectURL').and.returnValue('blob:synthetic'); const revoke = spyOn(URL, 'revokeObjectURL');
    const click = spyOn(HTMLAnchorElement.prototype, 'click');
    const fixture = TestBed.createComponent(HrReportsPageComponent); fixture.detectChanges(); const page = fixture.componentInstance;
    page.fromDate = '2026-01-01'; page.toDate = '2026-02-01';
    for (const report of page.reportTypes) { page.select(report.id); page.download(); expect(service.export).toHaveBeenCalledWith(report.id, page.fromDate, page.toDate); }
    expect(createUrl).toHaveBeenCalledTimes(5); expect(revoke).toHaveBeenCalledTimes(5); expect(click).toHaveBeenCalledTimes(5);
  });
  it('abrir y alternar pestañas conserva el asistente sin recrearlo ni recargar legacy', () => {
    const fixture = TestBed.createComponent(HrReportsPageComponent); fixture.detectChanges(); const page = fixture.componentInstance;
    expect(AssistantStub.created).toBe(0);
    page.openArea('assistant'); fixture.detectChanges(); expect(AssistantStub.created).toBe(1);
    page.openArea('operational'); fixture.detectChanges(); page.openArea('assistant'); fixture.detectChanges();
    expect(AssistantStub.created).toBe(1); expect(service.get).toHaveBeenCalledTimes(1);
    expect((fixture.nativeElement.querySelector('#operational-reports') as HTMLElement).hidden).toBeTrue();
    expect(fixture.nativeElement.querySelector('[role=tab][aria-selected=true]').textContent).toContain('Asistente');
  });
});
