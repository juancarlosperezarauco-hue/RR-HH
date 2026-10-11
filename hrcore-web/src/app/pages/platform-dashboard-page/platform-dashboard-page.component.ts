import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { PlatformDashboardUseCase } from '../../features/platform/application/platform-dashboard.usecase';
import { PlatformFacade } from '../../features/platform/lib/platform.facade';
import { PlatformStorageService } from '../../features/platform/lib/platform-storage.service';
import { ActivityItem, SuperadminDashboardResponse } from '../../entities/platform/api/platform-dashboard.model';

type SeverityLevel = 'info' | 'warn' | 'high';

interface ActivitySummaryCard {
  id: string;
  title: string;
  type: string;
  actorName: string;
  createdAt: string | null;
  summary: {
    name: string | null;
    slug: string | null;
    planCode: string | null;
  } | null;
  hasSummary: boolean;
}

@Component({
  selector: 'app-platform-dashboard-page',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="space-y-6">
      @if (isLoading()) {
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          @for (item of loadingSkeleton; track item) {
            <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
              <div class="h-4 w-24 rounded bg-accent animate-pulse"></div>
              <div class="mt-4 h-8 w-20 rounded bg-accent animate-pulse"></div>
              <div class="mt-3 h-3 w-32 rounded bg-accent animate-pulse"></div>
            </div>
          }
        </div>
      }

      @if (dashboard(); as data) {
        <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div class="space-y-3">
              <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                <span class="h-2 w-2 rounded-full bg-secondary"></span>
                Plataforma superadmin
              </div>
              <div>
                <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                  Bienvenido, {{ displayName() }}
                </h1>
                <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                  Vista ejecutiva del estado global de tenants, suscripciones, planes, usuarios y auditoría.
                </p>
              </div>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <div class="rounded-2xl border border-border bg-card/85 px-4 py-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">Generado el</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ formatDate(data.metadata.generatedAt) }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-card/85 px-4 py-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">Zona horaria</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ data.metadata.timezone }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-card/85 px-4 py-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">Cobertura</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ data.metadata.dataCompleteness }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-muted-foreground">Tenants</p>
              <lucide-icon name="building-2" class="h-5 w-5 text-primary"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-accent-foreground">{{ data.summary.tenants.total }}</p>
            <p class="mt-2 text-xs text-muted-foreground">Activos {{ data.summary.tenants.active }} | Inactivos {{ data.summary.tenants.inactive }}</p>
          </div>

          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-muted-foreground">Suscripciones</p>
              <lucide-icon name="badge-check" class="h-5 w-5 text-primary"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-accent-foreground">{{ data.summary.subscriptions.active }}</p>
            <p class="mt-2 text-xs text-muted-foreground">Trial {{ data.summary.subscriptions.trial }} | Vencidas {{ data.summary.subscriptions.expired }}</p>
          </div>

          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-muted-foreground">Planes activos</p>
              <lucide-icon name="sparkles" class="h-5 w-5 text-primary"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-accent-foreground">{{ data.summary.plans.active }}</p>
            <p class="mt-2 text-xs text-muted-foreground">Total registrados {{ data.summary.plans.total }}</p>
          </div>

          <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-muted-foreground">Usuarios plataforma</p>
              <lucide-icon name="users" class="h-5 w-5 text-primary"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-accent-foreground">{{ data.summary.users.total }}</p>
            <p class="mt-2 text-xs text-muted-foreground">Admins {{ data.summary.users.platformAdmins }} | Activos {{ data.summary.users.active }}</p>
          </div>
        </section>

        <section class="grid gap-4 xl:grid-cols-[1.35fr_0.95fr]">
          <div class="rounded-[24px] border border-border bg-card p-6 shadow-sm">
            <div class="flex items-center justify-between gap-4">
              <div>
                <h2 class="text-lg font-bold text-accent-foreground">Comparaciones del periodo</h2>
                <p class="text-sm text-muted-foreground">Evolución contra el periodo anterior</p>
              </div>
              <button (click)="reload()" class="rounded-full border border-border px-4 py-2 text-sm font-semibold text-primary hover:bg-accent transition-colors">
                Recargar
              </button>
            </div>

            <div class="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              @for (comparison of comparisonCards(); track comparison.label) {
                <div class="rounded-2xl border border-border bg-muted p-4">
                  <p class="text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">{{ comparison.label }}</p>
                  <p class="mt-3 text-2xl font-black" [class.text-accent-foreground]="comparison.value >= 0" [class.text-destructive]="comparison.value < 0">
                    {{ formatPercent(comparison.value) }}
                  </p>
                  <p class="mt-2 text-xs text-muted-foreground">{{ comparison.hint }}</p>
                </div>
              }
            </div>

            <div class="mt-6 grid gap-4 lg:grid-cols-2">
              <div>
                <h3 class="text-sm font-bold uppercase tracking-[0.14em] text-muted-foreground">Tenants por estado</h3>
                <div class="mt-3 space-y-3">
                  @for (item of data.tenants.byStatus; track item.status) {
                    <div class="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3">
                      <div>
                        <p class="text-sm font-semibold text-accent-foreground">{{ item.label }}</p>
                        <p class="text-xs text-muted-foreground">{{ item.status }}</p>
                      </div>
                      <p class="text-lg font-black text-primary">{{ item.total }}</p>
                    </div>
                  }
                </div>
              </div>

              <div>
                <h3 class="text-sm font-bold uppercase tracking-[0.14em] text-muted-foreground">Suscripciones por estado</h3>
                <div class="mt-3 space-y-3">
                  @for (item of data.subscriptions.byStatus; track item.status) {
                    <div class="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3">
                      <div>
                        <p class="text-sm font-semibold text-accent-foreground">{{ item.status }}</p>
                        <p class="text-xs text-muted-foreground">Estado general</p>
                      </div>
                      <p class="text-lg font-black text-primary">{{ item.total }}</p>
                    </div>
                  }
                </div>
              </div>
            </div>
          </div>

          <div class="rounded-[24px] border border-border bg-card p-6 shadow-sm">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-lg font-bold text-accent-foreground">Alertas</h2>
                <p class="text-sm text-muted-foreground">Señales que requieren atención</p>
              </div>
              <span class="rounded-full bg-accent px-3 py-1 text-xs font-bold text-primary">{{ data.alerts.length }}</span>
            </div>

            <div class="mt-4 space-y-3">
              @for (alert of data.alerts; track alert.id) {
                <div class="rounded-2xl border px-4 py-4"
                  [class.border-destructive/20]="severityLevel(alert.severity) === 'high'"
                  [class.bg-destructive/5]="severityLevel(alert.severity) === 'high'"
                  [class.border-warning/30]="severityLevel(alert.severity) === 'warn'"
                  [class.bg-warning/10]="severityLevel(alert.severity) === 'warn'"
                  [class.border-sky-200]="severityLevel(alert.severity) === 'info'"
                  [class.bg-sky-50]="severityLevel(alert.severity) === 'info'">
                  <div class="flex items-start justify-between gap-4">
                    <div>
                      <p class="text-sm font-bold text-accent-foreground">{{ alert.title }}</p>
                      <p class="mt-1 text-sm text-muted-foreground">{{ alert.description }}</p>
                    </div>
                    <span class="rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-[0.14em]"
                      [class.bg-destructive/10]="severityLevel(alert.severity) === 'high'"
                      [class.text-destructive]="severityLevel(alert.severity) === 'high'"
                      [class.bg-warning/10]="severityLevel(alert.severity) === 'warn'"
                      [class.text-warning]="severityLevel(alert.severity) === 'warn'"
                      [class.bg-sky-100]="severityLevel(alert.severity) === 'info'"
                      [class.text-sky-700]="severityLevel(alert.severity) === 'info'">
                      {{ alert.severity }}
                    </span>
                  </div>
                </div>
              }

              @if (data.alerts.length === 0) {
                <div class="rounded-2xl border border-dashed border-border bg-muted px-4 py-6 text-sm text-muted-foreground">
                  No hay alertas activas.
                </div>
              }
            </div>
          </div>
        </section>

        <section class="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
          <div class="min-w-0 rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
            <div class="flex items-center justify-between gap-3 min-w-0">
              <div>
                <h2 class="text-lg font-bold text-accent-foreground">Tenants recientes</h2>
                <p class="text-sm text-muted-foreground">Últimos registros creados en la plataforma</p>
              </div>
              <span class="shrink-0 text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">{{ data.tenants.recent.length }} items</span>
            </div>

            <div class="mt-4 overflow-x-auto rounded-2xl border border-border">
              <table class="min-w-[620px] w-full divide-y divide-border">
                <thead class="bg-background">
                  <tr class="text-left text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">
                    <th class="px-4 py-3">Tenant</th>
                    <th class="px-4 py-3">Slug</th>
                    <th class="px-4 py-3">Plan</th>
                    <th class="px-4 py-3">Estado</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-border bg-card">
                  @for (tenant of data.tenants.recent; track tenant.id) {
                    <tr>
                      <td class="px-4 py-3 min-w-0">
                        <p class="break-words font-semibold text-accent-foreground">{{ tenant.name }}</p>
                        <p class="break-words text-xs text-muted-foreground">{{ tenant.schemaName }}</p>
                      </td>
                      <td class="px-4 py-3 text-sm text-muted-foreground break-words">{{ tenant.slug }}</td>
                      <td class="px-4 py-3 text-sm text-muted-foreground break-words">{{ tenant.planCode || 'Sin plan' }}</td>
                      <td class="px-4 py-3">
                        <span class="rounded-full px-3 py-1 text-xs font-bold uppercase tracking-[0.1em]"
                          [class.bg-accent]="tenant.status === 'ACTIVE'"
                          [class.text-primary]="tenant.status === 'ACTIVE'"
                          [class.bg-destructive/10]="tenant.status !== 'ACTIVE'"
                          [class.text-destructive]="tenant.status !== 'ACTIVE'">
                          {{ tenant.status }}
                        </span>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>

          <div class="space-y-4 min-w-0">
            <div class="min-w-0 rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
              <div class="flex items-center justify-between gap-3 min-w-0">
                <div>
                  <h2 class="text-lg font-bold text-accent-foreground">Actividad reciente</h2>
                  <p class="text-sm text-muted-foreground">Eventos auditados más recientes</p>
                </div>
                <span class="shrink-0 text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">{{ recentActivityCards().length }} items</span>
              </div>

              <div class="mt-4 space-y-3">
                @for (activity of recentActivityCards(); track activity.id) {
                  <div class="rounded-2xl border border-border bg-muted p-4">
                    <div class="flex flex-col gap-3 min-w-0">
                      <div class="flex items-start justify-between gap-3 min-w-0">
                        <div class="min-w-0 flex-1">
                          <p class="break-words text-sm font-bold text-accent-foreground">{{ activity.title }}</p>
                        </div>
                        <span class="shrink-0 rounded-full bg-accent px-3 py-1 text-[11px] font-bold uppercase tracking-[0.12em] text-primary">
                          {{ activity.type }}
                        </span>
                      </div>

                      @if (activity.hasSummary && activity.summary; as summary) {
                        <div class="grid gap-2 sm:grid-cols-3">
                          @if (summary.name) {
                            <div class="rounded-xl border border-border bg-card px-3 py-2">
                              <p class="text-[10px] font-bold uppercase tracking-[0.12em] text-muted-foreground">Nombre</p>
                              <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ summary.name }}</p>
                            </div>
                          }

                          @if (summary.slug) {
                            <div class="rounded-xl border border-border bg-card px-3 py-2">
                              <p class="text-[10px] font-bold uppercase tracking-[0.12em] text-muted-foreground">Slug</p>
                              <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ summary.slug }}</p>
                            </div>
                          }

                          @if (summary.planCode) {
                            <div class="rounded-xl border border-border bg-card px-3 py-2">
                              <p class="text-[10px] font-bold uppercase tracking-[0.12em] text-muted-foreground">Plan</p>
                              <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ summary.planCode }}</p>
                            </div>
                          }
                        </div>
                      }

                      <p class="text-xs text-muted-foreground">{{ activity.actorName }} · {{ formatDate(activity.createdAt) }}</p>
                    </div>
                  </div>
                }
              </div>
            </div>

            <div class="min-w-0 rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
              <div class="flex items-center justify-between gap-3 min-w-0">
                <div>
                  <h2 class="text-lg font-bold text-accent-foreground">Insights</h2>
                  <p class="text-sm text-muted-foreground">Lecturas rápidas del sistema</p>
                </div>
              </div>

              <div class="mt-4 space-y-3 min-w-0">
                @for (insight of data.insights; track insight.type) {
                  <div class="rounded-2xl border border-border bg-muted px-4 py-4 min-w-0">
                    <p class="break-words text-sm font-bold text-accent-foreground">{{ insight.title }}</p>
                    <p class="mt-1 break-words text-sm text-muted-foreground">{{ insight.description }}</p>
                  </div>
                }
              </div>
            </div>
          </div>
        </section>
      }

      @if (error()) {
        <div class="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {{ error() }}
        </div>
      }
    </div>
  `
})
export class PlatformDashboardPageComponent implements OnInit {
  private readonly dashboardUseCase = inject(PlatformDashboardUseCase);
  private readonly platformFacade = inject(PlatformFacade);
  private readonly platformStorage = inject(PlatformStorageService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly dashboard = signal<SuperadminDashboardResponse | null>(null);
  readonly status = this.dashboardUseCase.status;
  readonly error = this.dashboardUseCase.error;
  readonly loadingSkeleton = [1, 2, 3, 4];

  readonly comparisonCards = computed<Array<{ label: string; value: number; hint: string }>>(() => {
    const summary = this.dashboard()?.comparisons?.summary;
    return [
      { label: 'Tenants', value: Number(summary?.tenantsChangePercent ?? 0), hint: 'vs periodo anterior' },
      { label: 'Suscripciones activas', value: Number(summary?.activeSubscriptionsChangePercent ?? 0), hint: 'vs periodo anterior' },
      { label: 'Nuevos tenants', value: Number(summary?.newTenantsChangePercent ?? 0), hint: 'vs periodo anterior' },
      { label: 'Suscripciones vencidas', value: Number(summary?.expiredSubscriptionsChangePercent ?? 0), hint: 'vs periodo anterior' }
    ];
  });

  readonly recentActivityCards = computed<ActivitySummaryCard[]>(() => {
    return (this.dashboard()?.recentActivity ?? []).map((activity) => {
      const summary = this.buildActivitySummary(activity.description);

      return {
        id: activity.id,
        title: activity.title,
        type: activity.type,
        actorName: activity.actorName || 'Sistema',
        createdAt: activity.createdAt,
        summary,
        hasSummary: summary !== null
      };
    });
  });

  ngOnInit(): void {
    void this.reload();
  }

  async reload(): Promise<void> {
    const dashboard = await this.dashboardUseCase.loadDashboard();
    this.dashboard.set(dashboard);
    this.cdr.detectChanges();
  }

  displayName(): string {
    const user = this.platformStorage.getUser() ?? this.platformFacade.currentSuperadmin();
    if (!user) {
      return 'SuperAdmin';
    }

    return `${user.firstName} ${user.lastName}`.trim();
  }

  isLoading(): boolean {
    return this.status() === 'loading';
  }

  formatPercent(value: number): string {
    const normalized = Number.isFinite(value) ? value : 0;
    const prefix = normalized > 0 ? '+' : '';
    return `${prefix}${normalized.toFixed(2)}%`;
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'Sin fecha';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'Fecha inválida';
    }

    return new Intl.DateTimeFormat('es-BO', {
      timeZone: 'America/La_Paz',
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  }

  buildActivitySummary(description: string | null | undefined): { name: string | null; slug: string | null; planCode: string | null } | null {
    if (!description) {
      return null;
    }

    try {
      const parsed = JSON.parse(description) as { name?: string; slug?: string; planCode?: string };
      const name = parsed.name?.trim();
      const slug = parsed.slug?.trim();
      const planCode = parsed.planCode?.trim();

      if (!name && !slug && !planCode) {
        return null;
      }

      return {
        name: name ?? null,
        slug: slug ?? null,
        planCode: planCode ?? null
      };
    } catch {
      return null;
    }
  }

  severityLevel(severity: string): SeverityLevel {
    const normalized = severity.toUpperCase();
    if (normalized === 'HIGH') return 'high';
    if (normalized === 'WARN' || normalized === 'MEDIUM') return 'warn';
    return 'info';
  }
}
