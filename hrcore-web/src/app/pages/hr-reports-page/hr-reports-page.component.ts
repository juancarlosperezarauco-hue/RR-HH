import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { DepartmentsService, Department } from '../../entities/departments';
import { EmployeesService, EmployeeSummary, EmploymentPeriod } from '../../entities/employees';
import { PositionsService, Position } from '../../entities/positions';
import { HrReportFilters, HrReportId, ReportsService } from '../../entities/reports';
import { ToastService } from '../../shared/ui/toast/toast.service';

type ReportTab = 'classic' | 'custom';
type ChartType = 'bar' | 'horizontal' | 'donut' | 'line' | 'area';
type ReportData = Record<string, unknown>;
interface ReportDefinition { id: HrReportId; label: string; description: string; statuses: { value: string; label: string }[]; }
interface Metric { label: string; value: string | number; }
interface ReportRow { label: string; value: number; }
interface ReportGroup { label: string; rows: ReportRow[]; }

@Component({
  selector: 'app-hr-reports-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  styles: [`
    @media print {
      .no-print { display: none !important; }
      .report-shell { padding: 0 !important; }
      .print-card { break-inside: avoid; box-shadow: none !important; }
    }
  `],
  template: `
    <section class="report-shell space-y-6 p-6">
      <header class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p class="text-sm font-medium text-primary">Recursos Humanos · información estadística del tenant</p>
          <h1 class="mt-1 text-3xl font-black tracking-tight text-foreground">Informe de RR. HH.</h1>
          <p class="mt-1 max-w-3xl text-sm text-muted-foreground">Consulta reportes estadísticos predeterminados o genera resultados aplicando criterios personalizados.</p>
        </div>
        <details class="no-print group relative" [class.pointer-events-none]="loading()" [class.opacity-50]="loading()">
          <summary class="flex cursor-pointer list-none items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90" aria-label="Mostrar formatos de descarga">
            Descargar
            <span aria-hidden="true" class="text-xs transition-transform group-open:rotate-180">▼</span>
          </summary>
          <div class="absolute right-0 z-20 mt-2 w-48 overflow-hidden rounded-lg border bg-card p-1 shadow-lg">
            <button type="button" class="flex w-full items-center rounded-md px-3 py-2 text-left text-sm hover:bg-muted" (click)="download('xlsx', $event)">Excel (.xlsx)</button>
            <button type="button" class="flex w-full items-center rounded-md px-3 py-2 text-left text-sm hover:bg-muted" (click)="download('docx', $event)">Word (.docx)</button>
            <button type="button" class="flex w-full items-center rounded-md px-3 py-2 text-left text-sm hover:bg-muted" (click)="download('pdf', $event)">PDF (.pdf)</button>
          </div>
        </details>
      </header>

      <nav class="no-print grid gap-2 rounded-xl border bg-card p-2 sm:grid-cols-2" aria-label="Tipos de reportes">
        <button *ngFor="let item of tabs" type="button" class="rounded-lg px-3 py-3 text-sm font-semibold transition-colors hover:bg-muted" [class.bg-primary]="tab()===item.id" [class.text-primary-foreground]="tab()===item.id" (click)="changeTab(item.id)">{{ item.label }}</button>
      </nav>

      <div *ngIf="error()" class="flex items-center justify-between gap-4 rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive" role="alert">
        <span>{{ error() }}</span><button type="button" class="no-print font-semibold underline" (click)="load()">Reintentar</button>
      </div>

      <ng-container *ngIf="tab()==='classic'">
        <section class="no-print rounded-xl border bg-card p-4">
          <h2 class="text-sm font-bold">Reportes estadísticos estáticos</h2>
          <p class="mt-1 text-xs text-muted-foreground">Selecciona un reporte fijo con indicadores calculados desde los módulos registrados.</p>
          <div class="mt-4 flex flex-wrap gap-2">
            <button *ngFor="let item of reportTypes" type="button" class="rounded-full border px-3 py-2 text-sm transition-colors hover:bg-muted" [class.bg-primary]="report()===item.id" [class.text-primary-foreground]="report()===item.id" (click)="select(item.id)">{{ item.label }}</button>
          </div>
        </section>
        <ng-container *ngTemplateOutlet="reportResult"></ng-container>
      </ng-container>

      <ng-container *ngIf="tab()==='custom'">
        <section class="no-print rounded-xl border bg-card p-5">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div><h2 class="font-bold">Reportes estadísticos paramétricos</h2><p class="mt-1 text-sm text-muted-foreground">Combina módulo, periodo, organización, colaborador y estado.</p></div>
            <button type="button" class="text-sm font-semibold text-primary hover:underline" (click)="clearFilters()">Limpiar criterios</button>
          </div>
          <div class="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <label class="text-sm font-medium">Tipo de reporte
              <select class="mt-1 w-full rounded-lg border bg-background px-3 py-2" [(ngModel)]="reportValue" (ngModelChange)="onReportChange($event)"><option *ngFor="let item of reportTypes" [value]="item.id">{{ item.label }}</option></select>
            </label>
            <label class="text-sm font-medium">Fecha inicial<input type="date" class="mt-1 w-full rounded-lg border bg-background px-3 py-2" [(ngModel)]="fromDate" /></label>
            <label class="text-sm font-medium">Fecha final<input type="date" class="mt-1 w-full rounded-lg border bg-background px-3 py-2" [(ngModel)]="toDate" /></label>
            <label class="text-sm font-medium">Estado
              <select class="mt-1 w-full rounded-lg border bg-background px-3 py-2" [(ngModel)]="status"><option value="">Todos</option><option *ngFor="let item of selectedDefinition().statuses" [value]="item.value">{{ item.label }}</option></select>
            </label>
            <label class="text-sm font-medium">Departamento
              <select class="mt-1 w-full rounded-lg border bg-background px-3 py-2" [(ngModel)]="departmentId"><option value="">Todos</option><option *ngFor="let item of departments()" [value]="item.id">{{ item.name }}</option></select>
            </label>
            <label class="text-sm font-medium">Cargo
              <select class="mt-1 w-full rounded-lg border bg-background px-3 py-2" [(ngModel)]="positionId"><option value="">Todos</option><option *ngFor="let item of positions()" [value]="item.id">{{ item.name }}</option></select>
            </label>
            <label class="text-sm font-medium">Empleado
              <select class="mt-1 w-full rounded-lg border bg-background px-3 py-2" [(ngModel)]="employeeId" (ngModelChange)="onEmployeeChange()"><option value="">Todos</option><option *ngFor="let item of employees()" [value]="item.id">{{ item.firstName }} {{ item.lastName }} · {{ item.employeeNumber }}</option></select>
            </label>
            <label class="text-sm font-medium">Periodo laboral
              <select class="mt-1 w-full rounded-lg border bg-background px-3 py-2 disabled:opacity-50" [(ngModel)]="employmentPeriodId" [disabled]="!employeeId"><option value="">Todos</option><option *ngFor="let item of periods()" [value]="item.id">Periodo {{ item.periodNumber }} · {{ item.startDate }}</option></select>
            </label>
          </div>
          <div class="mt-5 flex justify-end"><button type="button" class="rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50" [disabled]="loading()" (click)="load()">Generar reporte</button></div>
        </section>
        <ng-container *ngTemplateOutlet="reportResult"></ng-container>
      </ng-container>

      <ng-template #reportResult>
        <div *ngIf="loading()" class="rounded-xl border bg-card p-10 text-center text-sm text-muted-foreground">Generando reporte…</div>
        <ng-container *ngIf="!loading() && data()">
          <div class="no-print flex justify-end">
            <label class="text-sm font-medium">Tipo de gráfico
              <select class="ml-2 rounded-lg border bg-card px-3 py-2" [(ngModel)]="chartType">
                <option *ngFor="let option of chartTypes" [value]="option.value">{{ option.label }}</option>
              </select>
            </label>
          </div>
          <section class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
            <article *ngFor="let metric of metrics()" class="print-card rounded-xl border bg-card p-4"><p class="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{{ metric.label }}</p><p class="mt-2 text-2xl font-black tabular-nums">{{ metric.value }}</p></article>
          </section>
          <section class="grid gap-5 lg:grid-cols-2">
            <article *ngFor="let group of groups()" class="print-card rounded-xl border bg-card p-5">
              <h3 class="font-bold">{{ group.label }}</h3>
              <p *ngIf="!group.rows.length" class="mt-4 text-sm text-muted-foreground">Sin distribución disponible.</p>

              <div *ngIf="group.rows.length && chartType==='horizontal'" class="mt-4 space-y-3">
                <div *ngFor="let row of group.rows"><div class="mb-1 flex justify-between gap-4 text-sm"><span>{{ row.label }}</span><strong>{{ row.value }}</strong></div><div class="h-2 overflow-hidden rounded-full bg-muted"><div class="h-full rounded-full bg-primary" [style.width.%]="barWidth(row, group.rows)"></div></div></div>
              </div>

              <div *ngIf="group.rows.length && chartType==='bar'" class="mt-5 flex h-64 items-end gap-3 border-b border-l px-3 pb-2">
                <div *ngFor="let row of group.rows; let index=index" class="flex min-w-0 flex-1 flex-col items-center justify-end gap-2" [title]="row.label + ': ' + row.value">
                  <strong class="text-xs tabular-nums">{{ row.value }}</strong>
                  <div class="w-full max-w-12 rounded-t-md bg-primary" [style.height.px]="verticalBarHeight(row, group.rows)"></div>
                  <span class="w-full truncate text-center text-xs text-muted-foreground">{{ row.label }}</span>
                </div>
              </div>

              <div *ngIf="group.rows.length && chartType==='donut'" class="mt-5 grid items-center gap-5 sm:grid-cols-[180px_1fr]">
                <div class="relative mx-auto h-44 w-44 rounded-full" [style.background]="donutGradient(group.rows)"><div class="absolute inset-10 grid place-items-center rounded-full bg-card text-center"><span class="text-2xl font-black">{{ groupTotal(group.rows) }}</span><small class="text-muted-foreground">Total</small></div></div>
                <div class="space-y-2"><div *ngFor="let row of group.rows; let index=index" class="flex items-center justify-between gap-3 text-sm"><span class="flex min-w-0 items-center gap-2"><i class="h-3 w-3 shrink-0 rounded-full" [style.background]="chartColor(index)"></i><span class="truncate">{{ row.label }}</span></span><strong>{{ row.value }}</strong></div></div>
              </div>

              <div *ngIf="group.rows.length && (chartType==='line' || chartType==='area')" class="mt-5">
                <svg viewBox="0 0 100 100" preserveAspectRatio="none" class="h-56 w-full overflow-visible" role="img" [attr.aria-label]="group.label">
                  <line x1="5" y1="90" x2="95" y2="90" class="stroke-border" stroke-width="0.7"></line>
                  <line x1="5" y1="10" x2="5" y2="90" class="stroke-border" stroke-width="0.7"></line>
                  <path *ngIf="chartType==='area'" [attr.d]="areaPath(group.rows)" fill="rgba(37, 99, 235, 0.20)"></path>
                  <polyline [attr.points]="chartPoints(group.rows)" fill="none" stroke="rgb(37, 99, 235)" stroke-width="2" vector-effect="non-scaling-stroke" stroke-linecap="round" stroke-linejoin="round"></polyline>
                  <circle *ngFor="let row of group.rows; let index=index" [attr.cx]="pointX(index, group.rows.length)" [attr.cy]="pointY(row.value, group.rows)" r="1.8" fill="rgb(37, 99, 235)" vector-effect="non-scaling-stroke"><title>{{ row.label }}: {{ row.value }}</title></circle>
                </svg>
                <div class="mt-2 flex gap-2"><span *ngFor="let row of group.rows" class="min-w-0 flex-1 truncate text-center text-xs text-muted-foreground" [title]="row.label">{{ row.label }}</span></div>
              </div>
            </article>
          </section>
          <p *ngIf="!metrics().length && !groups().length" class="rounded-xl border bg-card p-10 text-center text-sm text-muted-foreground">El reporte no devolvió indicadores con los criterios seleccionados.</p>
        </ng-container>
      </ng-template>
    </section>
  `
})
export class HrReportsPageComponent implements OnInit {
  private readonly reports = inject(ReportsService);
  private readonly departmentsService = inject(DepartmentsService);
  private readonly positionsService = inject(PositionsService);
  private readonly employeesService = inject(EmployeesService);
  private readonly toast = inject(ToastService);

  readonly tabs: { id: ReportTab; label: string }[] = [
    { id: 'classic', label: 'Estáticos' }, { id: 'custom', label: 'Paramétricos' }
  ];
  readonly chartTypes: { value: ChartType; label: string }[] = [
    { value: 'bar', label: 'Gráfico de barras' },
    { value: 'horizontal', label: 'Barras horizontales' },
    { value: 'donut', label: 'Gráfico de anillos' },
    { value: 'line', label: 'Gráfico de línea' },
    { value: 'area', label: 'Gráfico de área' }
  ];
  readonly reportTypes: ReportDefinition[] = [
    { id: 'employees', label: 'Empleados', description: 'Estado y distribución de colaboradores', statuses: this.options(['ACTIVE','INACTIVE','ON_LEAVE','SUSPENDED','TERMINATED']) },
    { id: 'departments', label: 'Departamentos', description: 'Dotación, contratos y ausencias por área', statuses: this.options(['ACTIVE','INACTIVE']) },
    { id: 'positions', label: 'Cargos', description: 'Ocupación y salario promedio por cargo', statuses: this.options(['ACTIVE','INACTIVE']) },
    { id: 'contracts', label: 'Contratos', description: 'Vigencia y estado contractual', statuses: this.options(['ACTIVE','DRAFT','PENDING_SIGNATURE','EXPIRED','TERMINATED','CANCELLED']) },
    { id: 'files', label: 'Expedientes', description: 'Expedientes y documentos laborales', statuses: this.options(['ACTIVE','ARCHIVED']) },
    { id: 'shifts', label: 'Turnos', description: 'Turnos y asignaciones laborales', statuses: this.options(['ACTIVE','INACTIVE']) },
    { id: 'attendance', label: 'Asistencia', description: 'Marcaciones, tardanzas y horas trabajadas', statuses: this.options(['PRESENT','ABSENT','LATE','INCOMPLETE']) },
    { id: 'leaves', label: 'Ausencias y permisos', description: 'Solicitudes y días autorizados', statuses: this.options(['PENDING','APPROVED','REJECTED','CANCELLED']) },
    { id: 'incidents', label: 'Incidencias', description: 'Incidencias por tipo, gravedad y estado', statuses: this.options(['OPEN','UNDER_REVIEW','RESOLVED','CLOSED','CANCELLED']) },
    { id: 'integrated', label: 'Integrado de RR. HH.', description: 'Cruces entre contrato, asignación, expediente, turno y asistencia', statuses: [] }
  ];
  readonly tab = signal<ReportTab>('classic');
  readonly report = signal<HrReportId>('employees');
  readonly data = signal<ReportData | null>(null);
  readonly loading = signal(false); readonly error = signal('');
  readonly departments = signal<Department[]>([]); readonly positions = signal<Position[]>([]);
  readonly employees = signal<EmployeeSummary[]>([]); readonly periods = signal<EmploymentPeriod[]>([]);
  readonly selectedDefinition = computed(() => this.reportTypes.find(item => item.id === this.report())!);

  reportValue: HrReportId = 'employees';
  chartType: ChartType = 'horizontal';
  fromDate = ''; toDate = ''; departmentId = ''; positionId = ''; employeeId = ''; employmentPeriodId = ''; status = '';
  ngOnInit() { this.loadLookups(); this.load(); }
  changeTab(tab: ReportTab) { this.tab.set(tab); }
  select(id: HrReportId) { this.report.set(id); this.reportValue = id; this.status = ''; this.load(); }
  onReportChange(id: HrReportId) { this.report.set(id); this.reportValue = id; this.status = ''; }
  onEmployeeChange() {
    this.employmentPeriodId = ''; this.periods.set([]);
    if (!this.employeeId) return;
    this.employeesService.periods(this.employeeId).subscribe({ next: response => this.periods.set(response.data ?? []), error: () => this.toast.warning('No se pudieron cargar los periodos del empleado.') });
  }
  clearFilters() { this.fromDate='';this.toDate='';this.departmentId='';this.positionId='';this.employeeId='';this.employmentPeriodId='';this.status='';this.periods.set([]);this.load(); }
  load() {
    this.loading.set(true); this.error.set('');
    this.reports.get(this.report(), this.filters()).subscribe({
      next: response => { this.data.set(response.data); this.loading.set(false); },
      error: error => { this.loading.set(false); this.error.set(error?.error?.message ?? 'No se pudo generar el reporte.'); }
    });
  }
  metrics(): Metric[] {
    const data = this.data(); if (!data) return [];
    const keys: Record<HrReportId, { key: string; label: string; format?: (value: unknown) => string | number }[]> = {
      employees: [{key:'totalEmployees',label:'Total'},{key:'activeEmployees',label:'Activos'},{key:'inactiveEmployees',label:'Inactivos'},{key:'terminatedEmployees',label:'Desvinculados'},{key:'onLeaveEmployees',label:'En permiso'},{key:'employeesWithoutCurrentAssignment',label:'Sin asignación'}],
      departments: [{key:'totalDepartments',label:'Total'},{key:'activeDepartments',label:'Activos'},{key:'inactiveDepartments',label:'Inactivos'}],
      positions: [{key:'totalPositions',label:'Total'},{key:'activePositions',label:'Activos'},{key:'inactivePositions',label:'Inactivos'},{key:'positionsWithoutEmployees',label:'Sin empleados'}],
      contracts: [{key:'totalContracts',label:'Total'},{key:'activeContracts',label:'Activos'},{key:'expiredContracts',label:'Vencidos'},{key:'terminatedContracts',label:'Terminados'},{key:'renewedContracts',label:'Renovados'},{key:'expiringNext30Days',label:'Próximos a vencer'},{key:'averageSalary',label:'Salario promedio'}],
      files: [{key:'totalFiles',label:'Expedientes'},{key:'completeFiles',label:'Completos'},{key:'incompleteFiles',label:'Incompletos'},{key:'employeesWithoutFile',label:'Sin expediente'},{key:'pendingDocuments',label:'Documentos pendientes'},{key:'expiredDocuments',label:'Documentos vencidos'}],
      shifts: [{key:'totalShifts',label:'Turnos'},{key:'activeShifts',label:'Activos'},{key:'inactiveShifts',label:'Inactivos'},{key:'employeesWithShift',label:'Empleados con turno'},{key:'employeesWithoutShift',label:'Empleados sin turno'}],
      attendance: [{key:'totalRecords',label:'Registros'},{key:'presentRecords',label:'Asistencias'},{key:'absentRecords',label:'Faltas'},{key:'lateRecords',label:'Retrasos'},{key:'attendancePercentage',label:'Asistencia %'},{key:'punctualityPercentage',label:'Puntualidad %'},{key:'averageWorkedHours',label:'Promedio de horas'}],
      leaves: [{key:'totalRequests',label:'Solicitudes'},{key:'approvedRequests',label:'Aprobadas'},{key:'rejectedRequests',label:'Rechazadas'},{key:'pendingRequests',label:'Pendientes'},{key:'approvalPercentage',label:'Aprobación %'},{key:'totalApprovedDays',label:'Días de ausencia'}],
      incidents: [{key:'totalIncidents',label:'Incidencias'}],
      integrated: [{key:'employeesWithActiveContractAndShift',label:'Contrato y turno vigentes'},{key:'employeesWithoutActiveContract',label:'Sin contrato vigente'},{key:'employeesWithoutAssignment',label:'Sin departamento o cargo'},{key:'employeesWithoutShift',label:'Sin turno'},{key:'employeesWithoutFile',label:'Sin expediente'},{key:'contractsExpiringNext30Days',label:'Contratos próximos a vencer'}]
    };
    return keys[this.report()].filter(item => data[item.key] !== undefined).map(item => ({ label: item.label, value: item.format ? item.format(data[item.key]) : String(data[item.key]) }));
  }
  groups(): ReportGroup[] {
    const data = this.data(); if (!data) return [];
    const mapping: Record<HrReportId, { key: string; label: string }[]> = {
      employees:[{key:'byDepartment',label:'Por departamento'},{key:'byPosition',label:'Por cargo'},{key:'byContractType',label:'Por tipo de contrato'},{key:'monthlyHires',label:'Contrataciones por mes'}],
      departments:[{key:'byEmployeeCount',label:'Empleados por departamento'},{key:'contractsByDepartment',label:'Contratos por departamento'},{key:'absencesByDepartment',label:'Ausencias por departamento'}],
      positions:[{key:'byEmployeeCount',label:'Empleados por cargo'},{key:'averageSalaryByPosition',label:'Salario promedio por cargo'}],
      contracts:[{key:'byStatus',label:'Por estado'},{key:'byType',label:'Por tipo'},{key:'salaryByDepartment',label:'Salario promedio por departamento'},{key:'salaryByPosition',label:'Salario promedio por cargo'}],
      files:[{key:'byStatus',label:'Por estado'},{key:'byDocumentType',label:'Documentos por tipo'}],
      shifts:[{key:'byStatus',label:'Por estado'},{key:'byShift',label:'Empleados por turno'},{key:'byDayPeriod',label:'Turnos diurnos y nocturnos'}],
      attendance:[{key:'byStatus',label:'Por estado'},{key:'byDepartment',label:'Asistencia por departamento'},{key:'dailyTrend',label:'Evolución diaria'}],
      leaves:[{key:'byStatus',label:'Por estado'},{key:'byType',label:'Por tipo'},{key:'byDepartment',label:'Por departamento'},{key:'byPayment',label:'Remuneradas y no remuneradas'}],
      incidents:[{key:'byStatus',label:'Por estado'},{key:'byType',label:'Por tipo'},{key:'bySeverity',label:'Por gravedad'}],
      integrated:[{key:'attendanceByDepartment',label:'Asistencia por departamento'},{key:'absencesByDepartment',label:'Ausencias por departamento'}]
    };
    return mapping[this.report()].map(item => ({ label: item.label, rows: this.toRows(data[item.key]) }));
  }
  barWidth(row: ReportRow, rows: ReportRow[]) { const max=Math.max(1,...rows.map(item=>item.value)); return Math.max(row.value?4:0,(row.value/max)*100); }
  verticalBarHeight(row: ReportRow, rows: ReportRow[]) { const max=Math.max(1,...rows.map(item=>item.value)); return Math.max(row.value?8:0,(row.value/max)*180); }
  groupTotal(rows: ReportRow[]) { return rows.reduce((total,row)=>total+row.value,0); }
  chartColor(index: number) { return ['#2563eb','#0ea5e9','#14b8a6','#22c55e','#eab308','#f97316','#ef4444','#8b5cf6','#ec4899','#64748b'][index%10]; }
  donutGradient(rows: ReportRow[]) {
    const total=this.groupTotal(rows); if(total<=0)return 'conic-gradient(#e5e7eb 0deg 360deg)';
    let current=0;return `conic-gradient(${rows.map((row,index)=>{const start=current;current+=(row.value/total)*360;return `${this.chartColor(index)} ${start}deg ${current}deg`;}).join(',')})`;
  }
  pointX(index:number,length:number){return length<=1?50:5+(index/(length-1))*90;}
  pointY(value:number,rows:ReportRow[]){const max=Math.max(1,...rows.map(item=>item.value));return 90-(value/max)*75;}
  chartPoints(rows:ReportRow[]){return rows.map((row,index)=>`${this.pointX(index,rows.length)},${this.pointY(row.value,rows)}`).join(' ');}
  areaPath(rows:ReportRow[]){if(!rows.length)return '';const points=this.chartPoints(rows);return `M 5 90 L ${points} L 95 90 Z`;}
  download(format: 'xlsx' | 'docx' | 'pdf', event?: Event) {
    (event?.currentTarget as HTMLElement | null)?.closest('details')?.removeAttribute('open');
    this.reports.export(this.report(), format, this.filters()).subscribe({
      next: blob => { const url=URL.createObjectURL(blob);const anchor=document.createElement('a');anchor.href=url;anchor.download=`informe-${this.report()}-${new Date().toISOString().slice(0,10)}.${format}`;anchor.click();URL.revokeObjectURL(url); },
      error: error => this.toast.error(error?.error?.message ?? 'No se pudo exportar el reporte.')
    });
  }
  private loadLookups() {
    forkJoin({ departments:this.departmentsService.list(0,100),positions:this.positionsService.list(0,100),employees:this.employeesService.list(0,100) }).subscribe({
      next: response => {this.departments.set(response.departments.data.content);this.positions.set(response.positions.data.content);this.employees.set(response.employees.data.content);},
      error: () => this.toast.warning('Algunos filtros organizacionales no pudieron cargarse.')
    });
  }
  private filters():HrReportFilters{return{fromDate:this.fromDate||undefined,toDate:this.toDate||undefined,departmentId:this.departmentId||undefined,positionId:this.positionId||undefined,employeeId:this.employeeId||undefined,employmentPeriodId:this.employmentPeriodId||undefined,status:this.status||undefined};}
  private toRows(value:unknown):ReportRow[]{
    if(Array.isArray(value))return value.map(item=>{const row=item as Record<string,unknown>;return{label:this.label(String(row['label']??row['name']??row['status']??row['id']??'Sin nombre')),value:Number(row['total']??row['count']??row['value']??0)};});
    if(value&&typeof value==='object')return Object.entries(value as Record<string,unknown>).map(([label,value])=>({label:this.label(label),value:Number(value??0)}));return[];
  }
  private options(values:string[]){return values.map(value=>({value,label:this.label(value)}));}
  private label(value:string){return value.toLowerCase().replaceAll('_',' ').replace(/\b\w/g,char=>char.toUpperCase());}
}
