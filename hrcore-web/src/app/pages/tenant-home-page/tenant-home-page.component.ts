import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { BaseChartDirective } from 'ng2-charts';
import type { ChartOptions } from 'chart.js';
import { TenantDashboardSection, TenantDashboardUseCase } from '../../features/dashboard';
import type { TenantDashboardMetric } from '../../entities/dashboard/model/tenant-dashboard-response.model';
import { RoleListUseCase } from '../../features/role-management';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { TenantNavigationService } from '../../shared/lib/navigation/tenant-navigation.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { TenantTextKey } from '../../shared/lib/i18n/keys';

interface DashboardItem { key: string; label: string; value: unknown; }
interface DashboardGroup { key: string; label: string; max: number; items: DashboardItem[]; }
interface DashboardChart extends DashboardGroup { kind: 'bars' | 'donut'; title: string; }

@Component({
  selector: 'app-tenant-home-page',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, BaseChartDirective],
  template: `
    <div class="space-y-6">
      @if (auth.status() === 'loading' || auth.status() === 'idle' || dashboard.status() === 'loading') {
        <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4" aria-label="Cargando dashboard">
          @for (item of loadingSkeleton; track item) {
            <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
              <div class="h-4 w-28 animate-pulse rounded bg-accent"></div>
              <div class="mt-4 h-8 w-20 animate-pulse rounded bg-accent"></div>
              <div class="mt-3 h-3 w-36 animate-pulse rounded bg-accent"></div>
            </div>
          }
        </section>
      } @else if (dashboard.status() === 'error') {
        <section class="flex items-start gap-4 rounded-2xl border border-destructive/30 bg-destructive/10 p-6" role="alert">
          <lucide-icon name="triangle-alert" class="mt-0.5 h-5 w-5 shrink-0 text-destructive"></lucide-icon>
          <div>
            <h1 class="font-semibold text-destructive">No se pudo cargar el dashboard</h1>
            <p class="mt-1 text-sm text-destructive/80">{{ dashboard.error() }}</p>
            <button type="button" (click)="reload()" class="mt-3 inline-flex items-center gap-2 rounded-full border border-destructive/30 px-4 py-2 text-sm font-semibold text-destructive transition-colors hover:bg-destructive/10">
              <lucide-icon name="refresh-ccw" [size]="15"></lucide-icon>
              Intentar nuevamente
            </button>
          </div>
        </section>
      } @else if (auth.currentUser(); as user) {
        @if (dashboard.data(); as data) {
          <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
            <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
              <div class="space-y-3">
                <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                  <span class="h-2 w-2 rounded-full bg-secondary"></span>
                  Panel de {{ permissions.isCurrentOwner() ? 'propietario' : 'colaborador' }}
                </div>
                <div>
                  <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">Hola, {{ user.firstName }}</h1>
                  <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">Estado operativo de {{ data.metadata.tenantSlug }}. La información visible respeta tus permisos actuales.</p>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-3">
                <div class="rounded-2xl border border-border bg-card/85 px-4 py-3"><p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">Periodo</p><p class="mt-1 text-sm font-semibold text-accent-foreground">{{ formatDate(data.metadata.fromDate) }} – {{ formatDate(data.metadata.toDate) }}</p></div>
                <div class="rounded-2xl border border-border bg-card/85 px-4 py-3"><p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">Zona horaria</p><p class="mt-1 text-sm font-semibold text-accent-foreground">{{ data.metadata.timezone }}</p></div>
                <div class="rounded-2xl border border-border bg-card/85 px-4 py-3"><p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">Actualizado</p><p class="mt-1 text-sm font-semibold text-accent-foreground">{{ formatDateTime(data.metadata.generatedAt) }}</p></div>
              </div>
            </div>
          </section>

          @if (primaryKpis(data).length > 0) {
            <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Indicadores principales">
              @for (metric of primaryKpis(data); track metric.key) {
                <article class="rounded-2xl border border-border bg-card p-5 shadow-sm">
                  <div class="flex items-center justify-between gap-3"><p class="text-sm font-semibold text-muted-foreground">{{ metric.label }}</p><lucide-icon [name]="metricIcon(metric.key)" class="h-5 w-5 text-primary"></lucide-icon></div>
                  <p class="mt-4 text-3xl font-black text-accent-foreground">{{ displayValue(metric.value) }}</p>
                </article>
              }
            </section>
          }

          @if (charts(data).length > 0) {
            <section class="grid gap-4 xl:grid-cols-2" aria-label="Análisis operativo">
              @for (chart of charts(data); track chart.key) {
                @if ($index === 2 && activityBars(data).length > 0) {
                  <section class="xl:col-span-2 rounded-[24px] border border-border bg-card p-5 shadow-sm" aria-label="Actividad de recursos humanos">
                    <div class="flex items-start justify-between gap-4"><div><h2 class="text-lg font-bold text-accent-foreground">Actividad de RR. HH. del periodo</h2><p class="mt-1 text-sm text-muted-foreground">Volumen operativo por módulo</p></div><span class="rounded-full bg-accent px-3 py-1 text-xs font-semibold text-primary">Comparativa</span></div>
                    <div class="mt-6 h-72 w-full"><canvas baseChart type="bar" [data]="activityChartData(data)" [options]="activityChartOptions()"></canvas></div>
                  </section>
                }
                <article class="min-w-0 rounded-[24px] border border-border bg-card p-5 shadow-sm">
                  <div class="flex items-start justify-between gap-4"><div><h2 class="text-lg font-bold text-accent-foreground">{{ chart.title }}</h2><p class="mt-1 text-sm text-muted-foreground">Distribución del periodo seleccionado</p></div><span class="rounded-full bg-accent px-3 py-1 text-xs font-semibold text-primary">{{ chart.kind === 'donut' ? 'Distribución' : 'Comparativa' }}</span></div>
                  @if (chart.items.length === 0) {
                    <div class="mt-6 rounded-2xl border border-dashed border-border bg-muted p-6 text-center"><lucide-icon name="layout-dashboard" class="mx-auto h-7 w-7 text-muted-foreground"></lucide-icon><p class="mt-3 text-sm font-semibold text-accent-foreground">Sin registros en el periodo</p><p class="mt-1 text-xs text-muted-foreground">Este módulo no tiene datos para las fechas seleccionadas.</p></div>
                  } @else {
                    <div class="mt-5 h-64 w-full"><canvas baseChart [type]="chartType(chart)" [data]="chartData(chart)" [options]="chartOptions(chart)"></canvas></div>
                  }
                </article>
              }
            </section>
          } @else {
            <section class="rounded-2xl border border-dashed border-border bg-muted p-8 text-center"><lucide-icon name="layout-dashboard" class="mx-auto h-8 w-8 text-muted-foreground"></lucide-icon><h2 class="mt-3 font-semibold text-accent-foreground">No hay análisis disponibles para tu cuenta</h2><p class="mt-1 text-sm text-muted-foreground">Los análisis aparecen cuando tienes permisos de lectura en los módulos correspondientes.</p></section>
          }

          <section class="rounded-[24px] border border-border bg-card p-5 shadow-sm">
            <div class="flex items-center justify-between gap-4"><div><h2 class="text-lg font-bold text-accent-foreground">Acciones rápidas</h2><p class="mt-1 text-sm text-muted-foreground">Accede solo a los módulos habilitados para tu cuenta.</p></div><button type="button" (click)="reload()" class="inline-flex items-center gap-2 rounded-full border border-border px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent"><lucide-icon name="refresh-ccw" [size]="15"></lucide-icon>Recargar</button></div>
            <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              @for (item of quickActions(); track item.id) { <a [routerLink]="item.route" class="group rounded-2xl border border-border bg-muted p-4 transition-colors hover:border-primary hover:bg-accent"><div class="flex items-center justify-between gap-3"><span class="font-semibold text-accent-foreground">{{ item.label }}</span><lucide-icon name="arrow-right" [size]="16" class="text-primary transition-transform group-hover:translate-x-1"></lucide-icon></div><p class="mt-1 text-xs text-muted-foreground">Abrir módulo autorizado</p></a> } @empty { <p class="text-sm text-muted-foreground">No tienes módulos operativos asignados.</p> }
            </div>
          </section>
        }
      } @else { <section class="rounded-2xl border border-border bg-card p-8 text-sm text-muted-foreground">No pudimos verificar tu sesión.</section> }
    </div>
  `
})
export class TenantHomePageComponent implements OnInit {
  readonly auth = inject(AuthFacade);
  readonly permissions = inject(PermissionService);
  readonly navigation = inject(TenantNavigationService);
  private readonly languageState = inject(TenantLanguageStateService);
  t(key: TenantTextKey): string { return this.languageState.t(key); }
  readonly dashboard = inject(TenantDashboardUseCase);
  readonly roles = inject(RoleListUseCase);
  readonly loadingSkeleton = [1, 2, 3, 4];

  ngOnInit(): void {
    void this.dashboard.load();
    if (this.permissions.hasPermission('access.roles.read')) void this.roles.loadRoles(0, 200);
  }
  reload(): void {
    void this.dashboard.load();
    if (this.permissions.hasPermission('access.roles.read')) void this.roles.reloadRoles();
  }
  visibleMetrics(data: { metrics: TenantDashboardMetric[] }): TenantDashboardMetric[] { return data.metrics.filter(metric => this.permissions.hasPermission(metric.sourcePermission)); }
  visibleSections(data: { sections: TenantDashboardSection[] }): TenantDashboardSection[] { return data.sections.filter(section => this.permissions.hasPermission(section.sourcePermission)); }
  quickActions() {
    const priorityIds = new Set(['employees', 'users', 'contracts', 'attendance', 'incidents']);
    return this.navigation.visibleItems().filter(item => priorityIds.has(item.id));
  }
  primaryKpis(data: { metrics: TenantDashboardMetric[] }): TenantDashboardMetric[] {
    const metrics = this.visibleMetrics(data);
    const selected = [
      ['users.total', 'Usuarios con acceso'],
      ['employees.totalEmployees', 'Empleados registrados'],
      ['incidents.totalIncidents', 'Incidencias del periodo']
    ].map(([key, label]) => {
      const metric = metrics.find(item => item.key === key);
      return metric ? { ...metric, label } : null;
    }).filter((metric): metric is TenantDashboardMetric => Boolean(metric));
    if (this.permissions.hasPermission('access.roles.read') && this.roles.status() === 'success') {
      selected.push({ key: 'roles.total', label: 'Roles configurados', value: this.roles.data().length, sourcePermission: 'access.roles.read' });
    }
    return selected;
  }
  metricIcon(key: string): string { if (key.includes('employees') || key.includes('users')) return 'users'; if (key.includes('contracts')) return 'file-signature'; if (key.includes('attendance')) return 'clipboard-list'; if (key.includes('leaves')) return 'calendar'; if (key.includes('incidents')) return 'triangle-alert'; return 'layout-dashboard'; }
  chartType(chart: DashboardChart): any { return chart.kind === 'donut' ? 'doughnut' : 'bar'; }
  chartData(chart: DashboardChart): any {
    const colors = this.chartColors();
    return {
      labels: chart.items.map(item => item.label),
      datasets: [{
        data: chart.items.map(item => Number(item.value) || 0),
        backgroundColor: chart.kind === 'donut' ? colors : colors[0],
        borderColor: this.cssColor('--card'),
        borderWidth: chart.kind === 'donut' ? 2 : 0,
        borderRadius: chart.kind === 'bars' ? 7 : 0,
        barPercentage: 0.62,
        categoryPercentage: 0.72
      }]
    };
  }
  chartOptions(chart: DashboardChart): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 450 },
      plugins: {
        legend: { display: chart.kind === 'donut', position: 'bottom', labels: { color: this.cssColor('--muted-foreground'), usePointStyle: true, padding: 16 } },
        tooltip: { enabled: true }
      },
      scales: chart.kind === 'bars' ? {
        x: { ticks: { color: this.cssColor('--muted-foreground') }, grid: { display: false } },
        y: { beginAtZero: true, ticks: { color: this.cssColor('--muted-foreground'), precision: 0 }, grid: { color: this.cssColor('--border') } }
      } : undefined
    } as ChartOptions<'bar' | 'doughnut'>;
  }
  private chartColors(): string[] { return ['--primary', '--secondary', '--accent-foreground', '--muted-foreground', '--destructive'].map(token => this.cssColor(token)); }
  private cssColor(token: string): string { return typeof document === 'undefined' ? 'transparent' : getComputedStyle(document.documentElement).getPropertyValue(token).trim() || 'transparent'; }
  activityBars(data: { metrics: TenantDashboardMetric[] }): DashboardItem[] {
    const labels: [string, string][] = [
      ['employees.totalEmployees', 'Empleados'],
      ['contracts.totalContracts', 'Contratos'],
      ['attendance.totalRecords', 'Asistencia'],
      ['leaves.totalRequests', 'Permisos'],
      ['incidents.totalIncidents', 'Incidencias']
    ];
    return labels.map(([key, label]): DashboardItem | null => {
      const metric = this.visibleMetrics(data).find(item => item.key === key);
      return metric ? { key, label, value: Number(metric.value) || 0 } : null;
    }).filter((item): item is DashboardItem => item !== null);
  }
  activityChartData(data: { metrics: TenantDashboardMetric[] }): any {
    const colors = this.chartColors();
    return {
      labels: this.activityBars(data).map(item => item.label),
      datasets: [{
        label: 'Volumen del periodo',
        data: this.activityBars(data).map(item => Number(item.value) || 0),
        backgroundColor: colors[0],
        borderColor: colors[0],
        borderRadius: 8,
        borderSkipped: false,
        maxBarThickness: 56
      }]
    };
  }
  activityChartOptions(): any {
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 450 },
      plugins: {
        legend: { display: false },
        tooltip: { enabled: true }
      },
      scales: {
        x: { ticks: { color: this.cssColor('--muted-foreground') }, grid: { display: false } },
        y: { beginAtZero: true, ticks: { color: this.cssColor('--muted-foreground'), precision: 0 }, grid: { color: this.cssColor('--border') } }
      }
    };
  }
  charts(data: { sections: TenantDashboardSection[] }): DashboardChart[] {
    const charts: DashboardChart[] = [];
    for (const section of this.visibleSections(data)) {
      if (section.key === 'employees') {
        const statusItems = this.scalarGroup(section, [
          ['activeEmployees', 'Activos'], ['on_leaveEmployees', 'Con permiso'], ['inactiveEmployees', 'Inactivos'],
          ['suspendedEmployees', 'Suspendidos'], ['terminatedEmployees', 'Desvinculados']
        ]);
        charts.push(this.chart('employees-status', 'Empleados por estado', 'donut', statusItems));
      }
      if (section.key === 'users') {
        const statusItems = this.scalarGroup(section, [['active', 'Activos'], ['pending', 'Pendientes'], ['inactive', 'Inactivos']]);
        charts.push(this.chart('users-status', 'Usuarios por estado', 'donut', statusItems));
      }
      const arrays = Object.entries(section.data).filter(([key, value]) => Array.isArray(value) && ['byStatus', 'byType', 'bySeverity'].includes(key));
      for (const [key, value] of arrays) {
        const items = (value as unknown[]).map((item: any) => ({ key: String(item?.status ?? item?.type ?? item?.severity ?? item?.code ?? 'unknown'), label: this.label(String(item?.status ?? item?.type ?? item?.severity ?? item?.code ?? 'Sin categoría')), value: Number(item?.total ?? item?.count ?? 0) })).filter(item => Number.isFinite(item.value));
        const dimension = key === 'byStatus' ? 'estado' : key === 'byType' ? 'tipo' : 'severidad';
        charts.push(this.chart(`${section.key}-${key}`, `${section.title} por ${dimension}`, key === 'byStatus' ? 'donut' : 'bars', items));
      }
    }
    return charts;
  }
  scalarGroup(section: TenantDashboardSection, fields: [string, string][]): DashboardItem[] { return fields.map(([key, label]) => ({ key, label, value: Number(section.data[key] ?? 0) })); }
  chart(key: string, title: string, kind: 'bars' | 'donut', items: DashboardItem[]): DashboardChart { return { key, title, kind, items, label: title, max: Math.max(1, ...items.map(item => Number(item.value))) }; }
  sumValues = (sum: number, item: DashboardItem): number => sum + Number(item.value || 0);
  donutBackground(chart: DashboardChart): string { const total = chart.items.reduce(this.sumValues, 0) || 1; let offset = 0; const colors = ['var(--primary)', 'var(--secondary)', 'var(--accent-foreground)', 'var(--muted-foreground)', 'var(--destructive)']; const stops = chart.items.map((item, index) => { const next = offset + (Number(item.value) / total) * 360; const stop = `${colors[index % colors.length]} ${offset}deg ${next}deg`; offset = next; return stop; }); return `conic-gradient(${stops.join(', ')})`; }
  chartDotClass(index: number): string { return ['bg-primary', 'bg-secondary', 'bg-accent-foreground', 'bg-muted-foreground', 'bg-destructive'][index % 5]; }
  displayValue(value: unknown): string { if (typeof value === 'number') return new Intl.NumberFormat('es-BO').format(value); if (typeof value === 'string') return value; return '—'; }
  barWidth(value: unknown, max: number): number { const amount = Number(value); return max <= 0 || !Number.isFinite(amount) ? 0 : Math.max(8, Math.min(100, Math.round((amount / max) * 100))); }
  formatDate(value: string | null | undefined): string { if (!value) return 'Sin fecha'; const date = new Date(`${value}T00:00:00`); return Number.isNaN(date.getTime()) ? 'Fecha inválida' : new Intl.DateTimeFormat('es-BO', { dateStyle: 'medium' }).format(date); }
  formatDateTime(value: string | null | undefined): string { if (!value) return 'Sin fecha'; const date = new Date(value); return Number.isNaN(date.getTime()) ? 'Fecha inválida' : new Intl.DateTimeFormat('es-BO', { dateStyle: 'medium', timeStyle: 'short' }).format(date); }
  private label(value: string): string { return value.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ').replace(/\b\w/g, letter => letter.toUpperCase()); }
}
