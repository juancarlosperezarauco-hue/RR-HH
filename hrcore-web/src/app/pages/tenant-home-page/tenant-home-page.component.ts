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
interface RichKpiCard {
  key: string;
  label: string;
  value: string;
  subtitle: string;
  icon: string;
  route?: string;
}
interface OperationalHealthCard {
  key: string;
  title: string;
  percentage: number;
  summary: string;
  detail: string;
  icon: string;
}

@Component({
  selector: 'app-tenant-home-page',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, BaseChartDirective],
  template: `
    <div class="animate-fade-in space-y-6">
      @if (auth.status() === 'loading' || auth.status() === 'idle' || (dashboard.status() === 'loading' && !dashboard.data())) {
        <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-3" aria-label="Cargando dashboard">
          @for (item of loadingSkeleton; track item) {
            <div class="rounded-2xl border border-border bg-card p-5 shadow-xs">
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
            <button type="button" (click)="reload()" class="mt-3 inline-flex cursor-pointer items-center gap-2 rounded-full border border-destructive/30 px-4 py-2 text-sm font-semibold text-destructive transition-colors hover:bg-destructive/10">
              <lucide-icon name="refresh-ccw" [size]="15"></lucide-icon>
              Intentar nuevamente
            </button>
          </div>
        </section>
      } @else if (auth.currentUser(); as user) {
        @if (dashboard.data(); as data) {
          <!-- Cabecera Ejecutiva con Selector de Periodo -->
          <section class="animate-slide-up rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-accent/40 p-6 shadow-md">
            <div class="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
              <div class="space-y-2.5">
                <div class="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-card/90 px-3.5 py-1 text-xs font-bold uppercase tracking-[0.16em] text-primary shadow-2xs">
                  <span class="h-2 w-2 rounded-full bg-emerald-500"></span>
                  Panel de {{ permissions.isCurrentOwner() ? 'propietario' : 'colaborador' }} · {{ data.metadata.tenantSlug }}
                </div>
                <div>
                  <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">Hola, {{ user.firstName }}</h1>
                  <p class="mt-1.5 max-w-3xl text-sm leading-6 text-muted-foreground">
                    Resumen ejecutivo y métricas en tiempo real de recursos humanos, asistencia y operación laboral.
                  </p>
                </div>
              </div>

              <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
                <!-- Filtro rápido de rango de fechas conectado al backend -->
                <div class="inline-flex items-center rounded-2xl border border-border bg-card p-1 shadow-2xs">
                  @for (preset of rangePresets; track preset.days) {
                    <button
                      type="button"
                      (click)="selectRangePreset(preset.days)"
                      class="cursor-pointer rounded-xl px-3 py-1.5 text-xs font-bold transition-all duration-200"
                      [class.bg-primary]="selectedPresetDays === preset.days"
                      [class.text-primary-foreground]="selectedPresetDays === preset.days"
                      [class.shadow-xs]="selectedPresetDays === preset.days"
                      [class.text-muted-foreground]="selectedPresetDays !== preset.days"
                      [class.hover:text-primary]="selectedPresetDays !== preset.days">
                      {{ preset.label }}
                    </button>
                  }
                </div>

                <button
                  type="button"
                  (click)="reload()"
                  [disabled]="dashboard.status() === 'loading'"
                  class="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-2xl border border-border bg-card px-4 text-xs font-bold text-primary shadow-2xs transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:bg-accent disabled:opacity-50">
                  <lucide-icon name="refresh-ccw" [size]="14" [class.animate-spin]="dashboard.status() === 'loading'"></lucide-icon>
                  Actualizar
                </button>
              </div>
            </div>

            <div class="mt-5 grid gap-3 border-t border-border/70 pt-4 sm:grid-cols-3">
              <div class="flex items-center gap-3 rounded-2xl border border-border/80 bg-card/85 px-4 py-3">
                <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-accent text-primary">
                  <lucide-icon name="calendar" [size]="16"></lucide-icon>
                </div>
                <div>
                  <p class="text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">Periodo analizado</p>
                  <p class="mt-0.5 text-xs font-bold text-accent-foreground sm:text-sm">{{ formatDate(data.metadata.fromDate) }} – {{ formatDate(data.metadata.toDate) }}</p>
                </div>
              </div>
              <div class="flex items-center gap-3 rounded-2xl border border-border/80 bg-card/85 px-4 py-3">
                <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-accent text-primary">
                  <lucide-icon name="clock" [size]="16"></lucide-icon>
                </div>
                <div>
                  <p class="text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">Zona horaria</p>
                  <p class="mt-0.5 text-xs font-bold text-accent-foreground sm:text-sm">{{ data.metadata.timezone }}</p>
                </div>
              </div>
              <div class="flex items-center gap-3 rounded-2xl border border-border/80 bg-card/85 px-4 py-3">
                <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-accent text-primary">
                  <lucide-icon name="check-circle" [size]="16"></lucide-icon>
                </div>
                <div>
                  <p class="text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">Última sincronización</p>
                  <p class="mt-0.5 text-xs font-bold text-accent-foreground sm:text-sm">{{ formatDateTime(data.metadata.generatedAt) }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Indicadores Principales Enriquecidos -->
          @if (richKpis(data).length > 0) {
            <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-label="Indicadores principales">
              @for (kpi of richKpis(data); track kpi.key) {
                <article class="group relative overflow-hidden rounded-2xl border border-border bg-card p-5 shadow-xs transition-all duration-300 hover:-translate-y-1 hover:border-primary/30 hover:shadow-md">
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <p class="text-xs font-bold uppercase tracking-wider text-muted-foreground">{{ kpi.label }}</p>
                      <p class="mt-2.5 text-3xl font-black tracking-tight text-accent-foreground">{{ kpi.value }}</p>
                    </div>
                    <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-accent text-primary transition-transform duration-300 group-hover:scale-110 group-hover:bg-primary group-hover:text-primary-foreground">
                      <lucide-icon [name]="kpi.icon" class="h-5 w-5"></lucide-icon>
                    </div>
                  </div>
                  <div class="mt-3 flex items-center justify-between border-t border-border/60 pt-2.5 text-xs">
                    <span class="font-medium text-muted-foreground">{{ kpi.subtitle }}</span>
                    @if (kpi.route) {
                      <a [routerLink]="kpi.route" class="inline-flex items-center gap-1 font-bold text-primary hover:underline">
                        Ver
                        <lucide-icon name="arrow-right" [size]="12"></lucide-icon>
                      </a>
                    }
                  </div>
                </article>
              }
            </section>
          }

          <!-- Indicadores de Salud Operativa de RR. HH. -->
          @if (operationalHealth(data).length > 0) {
            <section class="rounded-[24px] border border-border bg-card p-5 shadow-xs sm:p-6" aria-label="Salud operativa de recursos humanos">
              <div class="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 class="text-lg font-black text-accent-foreground">Salud operativa e indicadores clave</h2>
                  <p class="text-xs text-muted-foreground sm:text-sm">Tasas de cumplimiento y cobertura calculadas en tiempo real desde la base de datos.</p>
                </div>
                <span class="inline-flex w-fit items-center gap-1.5 rounded-full bg-accent px-3 py-1 text-xs font-bold text-primary">
                  <lucide-icon name="bar-chart-3" [size]="14"></lucide-icon>
                  Métricas en vivo
                </span>
              </div>

              <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                @for (item of operationalHealth(data); track item.key) {
                  <div class="rounded-2xl border border-border bg-muted/40 p-4 transition-all duration-200 hover:border-primary/30 hover:bg-card">
                    <div class="flex items-center justify-between gap-2">
                      <span class="text-xs font-bold text-foreground">{{ item.title }}</span>
                      <span class="rounded-lg bg-primary/10 px-2 py-0.5 text-xs font-black text-primary">{{ item.percentage }}%</span>
                    </div>
                    <div class="mt-2.5 h-2 w-full overflow-hidden rounded-full bg-border">
                      <div
                        class="h-full rounded-full bg-gradient-to-r from-primary to-secondary transition-all duration-500"
                        [style.width.%]="item.percentage">
                      </div>
                    </div>
                    <p class="mt-2.5 text-xs font-bold text-accent-foreground">{{ item.summary }}</p>
                    <p class="mt-0.5 text-[11px] text-muted-foreground">{{ item.detail }}</p>
                  </div>
                }
              </div>
            </section>
          }

          @if (charts(data).length > 0) {
            <section class="grid gap-4 xl:grid-cols-2" aria-label="Análisis operativo">
              @for (chart of charts(data); track chart.key) {
                @if ($index === 2 && activityBars(data).length > 0) {
                  <section class="xl:col-span-2 rounded-[24px] border border-border bg-card p-5 shadow-xs transition-all duration-300 hover:shadow-md" aria-label="Actividad de recursos humanos">
                    <div class="flex items-start justify-between gap-4">
                      <div>
                        <h2 class="text-lg font-bold text-accent-foreground">Actividad de RR. HH. del periodo</h2>
                        <p class="mt-1 text-sm text-muted-foreground">Volumen operativo consolidado por módulo</p>
                      </div>
                      <span class="rounded-full bg-accent px-3 py-1 text-xs font-semibold text-primary">Comparativa</span>
                    </div>
                    <div class="mt-6 h-72 w-full">
                      <canvas baseChart type="bar" [data]="activityChartData(data)" [options]="activityChartOptions()"></canvas>
                    </div>
                  </section>
                }
                <article class="min-w-0 rounded-[24px] border border-border bg-card p-5 shadow-xs transition-all duration-300 hover:shadow-md">
                  <div class="flex items-start justify-between gap-4">
                    <div>
                      <h2 class="text-lg font-bold text-accent-foreground">{{ chart.title }}</h2>
                      <p class="mt-1 text-sm text-muted-foreground">Distribución del periodo seleccionado</p>
                    </div>
                    <span class="rounded-full bg-accent px-3 py-1 text-xs font-semibold text-primary">
                      {{ chart.kind === 'donut' ? 'Distribución' : 'Comparativa' }}
                    </span>
                  </div>
                  @if (chart.items.length === 0) {
                    <div class="mt-6 rounded-2xl border border-dashed border-border bg-muted p-6 text-center">
                      <lucide-icon name="layout-dashboard" class="mx-auto h-7 w-7 text-muted-foreground"></lucide-icon>
                      <p class="mt-3 text-sm font-semibold text-accent-foreground">Sin registros en el periodo</p>
                      <p class="mt-1 text-xs text-muted-foreground">Este módulo no tiene datos para las fechas seleccionadas.</p>
                    </div>
                  } @else {
                    <div class="mt-5 h-64 w-full">
                      <canvas baseChart [type]="chartType(chart)" [data]="chartData(chart)" [options]="chartOptions(chart)"></canvas>
                    </div>
                  }
                </article>
              }
            </section>
          } @else {
            <section class="rounded-2xl border border-dashed border-border bg-muted p-8 text-center">
              <lucide-icon name="layout-dashboard" class="mx-auto h-8 w-8 text-muted-foreground"></lucide-icon>
              <h2 class="mt-3 font-semibold text-accent-foreground">No hay análisis disponibles para tu cuenta</h2>
              <p class="mt-1 text-sm text-muted-foreground">Los análisis aparecen cuando tienes permisos de lectura en los módulos correspondientes.</p>
            </section>
          }

          <!-- Acciones Rápidas Mejoradas -->
          <section class="rounded-[24px] border border-border bg-card p-5 shadow-xs sm:p-6">
            <div class="flex items-center justify-between gap-4">
              <div>
                <h2 class="text-lg font-black text-accent-foreground">Accesos rápidos a módulos</h2>
                <p class="mt-1 text-sm text-muted-foreground">Navega directamente a las áreas operativas habilitadas para tu rol.</p>
              </div>
            </div>
            <div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              @for (item of quickActions(); track item.id) {
                <a
                  [routerLink]="item.route"
                  class="group flex items-center gap-3.5 rounded-2xl border border-border bg-muted/50 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/40 hover:bg-accent hover:shadow-sm">
                  <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-card text-primary shadow-2xs transition-colors duration-200 group-hover:bg-primary group-hover:text-primary-foreground">
                    <lucide-icon [name]="item.icon" [size]="18"></lucide-icon>
                  </div>
                  <div class="min-w-0 flex-1">
                    <div class="flex items-center justify-between gap-2">
                      <span class="truncate font-bold text-accent-foreground group-hover:text-primary">{{ item.label }}</span>
                      <lucide-icon name="arrow-right" [size]="15" class="shrink-0 text-primary transition-transform duration-200 group-hover:translate-x-1"></lucide-icon>
                    </div>
                    <p class="mt-0.5 truncate text-xs text-muted-foreground">Gestionar {{ item.label.toLowerCase() }}</p>
                  </div>
                </a>
              } @empty {
                <p class="text-sm text-muted-foreground">No tienes módulos operativos asignados.</p>
              }
            </div>
          </section>
        }
      } @else {
        <section class="rounded-2xl border border-border bg-card p-8 text-sm text-muted-foreground">No pudimos verificar tu sesión.</section>
      }
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
  readonly loadingSkeleton = [1, 2, 3, 4, 5, 6];

  selectedPresetDays = 30;
  readonly rangePresets = [
    { label: '30 días', days: 30 },
    { label: '90 días', days: 90 },
    { label: '365 días', days: 365 }
  ];

  ngOnInit(): void {
    this.loadWithCurrentPreset();
    if (this.permissions.hasPermission('access.roles.read')) void this.roles.loadRoles(0, 200);
  }

  selectRangePreset(days: number): void {
    if (this.selectedPresetDays === days && this.dashboard.status() === 'success') return;
    this.selectedPresetDays = days;
    this.loadWithCurrentPreset();
  }

  reload(): void {
    this.loadWithCurrentPreset();
    if (this.permissions.hasPermission('access.roles.read')) void this.roles.reloadRoles();
  }

  private loadWithCurrentPreset(): void {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - this.selectedPresetDays);
    const toStr = to.toISOString().slice(0, 10);
    const fromStr = from.toISOString().slice(0, 10);
    void this.dashboard.load(fromStr, toStr);
  }

  visibleMetrics(data: { metrics: TenantDashboardMetric[] }): TenantDashboardMetric[] {
    return data.metrics.filter(metric => this.permissions.hasPermission(metric.sourcePermission));
  }

  visibleSections(data: { sections: TenantDashboardSection[] }): TenantDashboardSection[] {
    return data.sections.filter(section => this.permissions.hasPermission(section.sourcePermission));
  }

  quickActions() {
    const priorityIds = new Set(['employees', 'users', 'departments', 'positions', 'contracts', 'shifts', 'attendance', 'absences', 'incidents', 'hr-reports']);
    return this.navigation.visibleItems().filter(item => priorityIds.has(item.id));
  }

  richKpis(data: { metrics: TenantDashboardMetric[]; sections: TenantDashboardSection[] }): RichKpiCard[] {
    const metrics = this.visibleMetrics(data);
    const getMetricVal = (key: string): number | null => {
      const found = metrics.find(m => m.key === key);
      return found !== undefined ? Number(found.value ?? 0) : null;
    };

    const cards: RichKpiCard[] = [];

    const totalUsers = getMetricVal('users.total');
    if (totalUsers !== null) {
      const pendingUsers = getMetricVal('users.pending') ?? 0;
      cards.push({
        key: 'users.total',
        label: 'Usuarios del sistema',
        value: this.displayValue(totalUsers),
        subtitle: pendingUsers > 0 ? `${pendingUsers} pendientes de activación` : 'Todas las cuentas verificadas',
        icon: 'users',
        route: '/dashboard/users'
      });
    }

    const totalEmp = getMetricVal('employees.totalEmployees');
    if (totalEmp !== null) {
      const activeEmp = getMetricVal('employees.activeEmployees') ?? 0;
      const onLeaveEmp = getMetricVal('employees.on_leaveEmployees') ?? 0;
      cards.push({
        key: 'employees.totalEmployees',
        label: 'Empleados registrados',
        value: this.displayValue(totalEmp),
        subtitle: `${activeEmp} activos · ${onLeaveEmp} con licencia`,
        icon: 'contact-round',
        route: '/dashboard/employees'
      });
    }

    const totalContracts = getMetricVal('contracts.totalContracts');
    if (totalContracts !== null) {
      const activeContracts = getMetricVal('contracts.activeContracts') ?? 0;
      const pendingSig = getMetricVal('contracts.pending_signatureContracts') ?? 0;
      cards.push({
        key: 'contracts.totalContracts',
        label: 'Contratos laborales',
        value: this.displayValue(totalContracts),
        subtitle: `${activeContracts} vigentes · ${pendingSig} por firmar`,
        icon: 'file-signature',
        route: '/dashboard/contracts'
      });
    }

    const totalAttendance = getMetricVal('attendance.totalRecords');
    if (totalAttendance !== null) {
      const lateMins = getMetricVal('attendance.totalLateMinutes') ?? 0;
      const overtimeMins = getMetricVal('attendance.approvedOvertimeMinutes') ?? 0;
      cards.push({
        key: 'attendance.totalRecords',
        label: 'Registros de asistencia',
        value: this.displayValue(totalAttendance),
        subtitle: `${lateMins} min tardanza · ${overtimeMins} min extra`,
        icon: 'clock',
        route: '/dashboard/attendance'
      });
    }

    const totalLeaves = getMetricVal('leaves.totalRequests');
    if (totalLeaves !== null) {
      const approvedDays = getMetricVal('leaves.approvedDays') ?? 0;
      const pendingDays = getMetricVal('leaves.pendingDays') ?? 0;
      cards.push({
        key: 'leaves.totalRequests',
        label: 'Solicitudes de ausencia',
        value: this.displayValue(totalLeaves),
        subtitle: `${approvedDays} días aprobados · ${pendingDays} pendientes`,
        icon: 'calendar',
        route: '/dashboard/absences'
      });
    }

    const totalIncidents = getMetricVal('incidents.totalIncidents');
    if (totalIncidents !== null) {
      cards.push({
        key: 'incidents.totalIncidents',
        label: 'Incidencias del periodo',
        value: this.displayValue(totalIncidents),
        subtitle: totalIncidents === 0 ? 'Sin incidencias reportadas' : 'Casos registrados en el periodo',
        icon: 'triangle-alert',
        route: '/dashboard/incidents'
      });
    } else if (this.permissions.hasPermission('access.roles.read') && this.roles.status() === 'success') {
      cards.push({
        key: 'roles.total',
        label: 'Roles configurados',
        value: this.displayValue(this.roles.data().length),
        subtitle: 'Control de acceso RBAC activo',
        icon: 'shield-check',
        route: '/dashboard/roles'
      });
    }

    return cards;
  }

  operationalHealth(data: { sections: TenantDashboardSection[] }): OperationalHealthCard[] {
    const sections = this.visibleSections(data);
    const items: OperationalHealthCard[] = [];

    const empSec = sections.find(s => s.key === 'employees');
    if (empSec) {
      const total = Number(empSec.data['totalEmployees'] ?? 0);
      const active = Number(empSec.data['activeEmployees'] ?? 0);
      const unassigned = Number(empSec.data['employeesWithoutCurrentAssignment'] ?? 0);
      const activePct = total > 0 ? Math.round((active / total) * 100) : 100;
      const assigned = Math.max(0, total - unassigned);
      const assignedPct = total > 0 ? Math.round((assigned / total) * 100) : 100;

      items.push({
        key: 'emp-active',
        title: 'Plantilla activa',
        percentage: activePct,
        summary: `${active} de ${total} colaboradores activos`,
        detail: 'Disponibilidad operativa actual',
        icon: 'users'
      });

      items.push({
        key: 'emp-assigned',
        title: 'Cobertura de cargos',
        percentage: assignedPct,
        summary: `${assigned} con cargo y área asignados`,
        detail: unassigned > 0 ? `${unassigned} sin asignación vigente` : '100% de plantilla asignada',
        icon: 'briefcase'
      });
    }

    const conSec = sections.find(s => s.key === 'contracts');
    if (conSec) {
      const total = Number(conSec.data['totalContracts'] ?? 0);
      const active = Number(conSec.data['activeContracts'] ?? 0);
      const expired = Number(conSec.data['expiredContracts'] ?? 0);
      const pct = total > 0 ? Math.round((active / total) * 100) : 100;
      items.push({
        key: 'con-active',
        title: 'Contratos vigentes',
        percentage: pct,
        summary: `${active} de ${total} contratos activos`,
        detail: expired > 0 ? `${expired} contratos vencidos` : 'Sin contratos vencidos',
        icon: 'file-signature'
      });
    }

    const userSec = sections.find(s => s.key === 'users');
    if (userSec) {
      const total = Number(userSec.data['total'] ?? 0);
      const active = Number(userSec.data['active'] ?? 0);
      const pending = Number(userSec.data['pending'] ?? 0);
      const pct = total > 0 ? Math.round((active / total) * 100) : 100;
      items.push({
        key: 'usr-active',
        title: 'Cuentas habilitadas',
        percentage: pct,
        summary: `${active} de ${total} cuentas activas`,
        detail: pending > 0 ? `${pending} pendientes de activación` : 'Todas las cuentas verificadas',
        icon: 'shield-check'
      });
    }

    return items.slice(0, 4);
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
  private label(value: string): string {
    const dictionary: Record<string, string> = {
      ACTIVE: 'Activo',
      INACTIVE: 'Inactivo',
      PENDING: 'Pendiente',
      PENDING_SIGNATURE: 'Pendiente de firma',
      DRAFT: 'Borrador',
      EXPIRED: 'Vencido',
      TERMINATED: 'Finalizado / Desvinculado',
      CANCELLED: 'Cancelado',
      ON_LEAVE: 'Con permiso',
      SUSPENDED: 'Suspendido',
      INDEFINITE: 'Tiempo indefinido',
      FIXED_TERM: 'Plazo fijo',
      SEASONAL: 'Temporada',
      WORK_OR_SERVICE: 'Obra o servicio',
      EVENTUAL: 'Eventual',
      INTERNSHIP: 'Pasantía',
      PRESENT: 'Presente',
      LATE: 'Con tardanza',
      ABSENT: 'Ausente',
      INCOMPLETE: 'Incompleto',
      REGULARIZED: 'Regularizado',
      HOLIDAY: 'Feriado',
      LEAVE: 'Con licencia',
      APPROVED: 'Aprobado',
      REJECTED: 'Rechazado',
      OPEN: 'Abierta',
      UNDER_REVIEW: 'En revisión',
      RESOLVED: 'Resuelta',
      CLOSED: 'Cerrada',
      LOW: 'Baja',
      MEDIUM: 'Media',
      HIGH: 'Alta',
      CRITICAL: 'Crítica',
      MEMORANDUM: 'Memorándum',
      WARNING: 'Amonestación',
      DISCIPLINARY: 'Disciplinaria',
      MISCONDUCT: 'Falta de conducta',
      PERFORMANCE: 'Desempeño',
      SAFETY: 'Seguridad',
      RECOGNITION: 'Reconocimiento',
      OTHER: 'Otro'
    };
    const upper = (value || '').trim().toUpperCase();
    if (dictionary[upper]) return dictionary[upper];
    return value.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ').replace(/\b\w/g, letter => letter.toUpperCase());
  }
}
