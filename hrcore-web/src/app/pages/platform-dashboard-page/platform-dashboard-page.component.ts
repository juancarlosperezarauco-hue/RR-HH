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
            <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
              <div class="h-4 w-24 rounded bg-[#E8F5E9] animate-pulse"></div>
              <div class="mt-4 h-8 w-20 rounded bg-[#E8F5E9] animate-pulse"></div>
              <div class="mt-3 h-3 w-32 rounded bg-[#E8F5E9] animate-pulse"></div>
            </div>
          }
        </div>
      }

      @if (dashboard(); as data) {
        <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div class="space-y-3">
              <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
                <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
                Plataforma superadmin
              </div>
              <div>
                <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                  Bienvenido, {{ displayName() }}
                </h1>
                <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                  Vista ejecutiva del estado global de tenants, suscripciones, planes, usuarios y auditoría.
                </p>
              </div>
            </div>

            <div class="grid gap-3 sm:grid-cols-3">
              <div class="rounded-2xl border border-[#C8E6C9] bg-white/85 px-4 py-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#6B7D6C]">Generado el</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ formatDate(data.metadata.generatedAt) }}</p>
              </div>
              <div class="rounded-2xl border border-[#C8E6C9] bg-white/85 px-4 py-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#6B7D6C]">Zona horaria</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ data.metadata.timezone }}</p>
              </div>
              <div class="rounded-2xl border border-[#C8E6C9] bg-white/85 px-4 py-3">
                <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#6B7D6C]">Cobertura</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ data.metadata.dataCompleteness }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-[#567157]">Tenants</p>
              <lucide-icon name="building-2" class="h-5 w-5 text-[#2E7D32]"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ data.summary.tenants.total }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Activos {{ data.summary.tenants.active }} | Inactivos {{ data.summary.tenants.inactive }}</p>
          </div>

          <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-[#567157]">Suscripciones</p>
              <lucide-icon name="badge-check" class="h-5 w-5 text-[#2E7D32]"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ data.summary.subscriptions.active }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Trial {{ data.summary.subscriptions.trial }} | Vencidas {{ data.summary.subscriptions.expired }}</p>
          </div>

          <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-[#567157]">Planes activos</p>
              <lucide-icon name="sparkles" class="h-5 w-5 text-[#2E7D32]"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ data.summary.plans.active }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Total registrados {{ data.summary.plans.total }}</p>
          </div>

          <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
            <div class="flex items-center justify-between">
              <p class="text-sm font-semibold text-[#567157]">Usuarios plataforma</p>
              <lucide-icon name="users" class="h-5 w-5 text-[#2E7D32]"></lucide-icon>
            </div>
            <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ data.summary.users.total }}</p>
            <p class="mt-2 text-xs text-[#6B7D6C]">Admins {{ data.summary.users.platformAdmins }} | Activos {{ data.summary.users.active }}</p>
          </div>
        </section>

        <section class="grid gap-4 xl:grid-cols-[1.35fr_0.95fr]">
          <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-6 shadow-sm">
            <div class="flex items-center justify-between gap-4">
              <div>
                <h2 class="text-lg font-bold text-[#1B5E20]">Comparaciones del periodo</h2>
                <p class="text-sm text-[#6B7D6C]">Evolución contra el periodo anterior</p>
              </div>
              <button (click)="reload()" class="rounded-full border border-[#C8E6C9] px-4 py-2 text-sm font-semibold text-[#2E7D32] hover:bg-[#F1F8E9] transition-colors">
                Recargar
              </button>
            </div>

            <div class="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              @for (comparison of comparisonCards(); track comparison.label) {
                <div class="rounded-2xl border border-[#E2EEDC] bg-[#FAFCF8] p-4">
                  <p class="text-xs font-semibold uppercase tracking-[0.14em] text-[#6B7D6C]">{{ comparison.label }}</p>
                  <p class="mt-3 text-2xl font-black" [class.text-[#1B5E20]]="comparison.value >= 0" [class.text-[#C62828]]="comparison.value < 0">
                    {{ formatPercent(comparison.value) }}
                  </p>
                  <p class="mt-2 text-xs text-[#6B7D6C]">{{ comparison.hint }}</p>
                </div>
              }
            </div>

            <div class="mt-6 grid gap-4 lg:grid-cols-2">
              <div>
                <h3 class="text-sm font-bold uppercase tracking-[0.14em] text-[#6B7D6C]">Tenants por estado</h3>
                <div class="mt-3 space-y-3">
                  @for (item of data.tenants.byStatus; track item.status) {
                    <div class="flex items-center justify-between rounded-xl border border-[#E8F2E2] bg-white px-4 py-3">
                      <div>
                        <p class="text-sm font-semibold text-[#1B5E20]">{{ item.label }}</p>
                        <p class="text-xs text-[#6B7D6C]">{{ item.status }}</p>
                      </div>
                      <p class="text-lg font-black text-[#2E7D32]">{{ item.total }}</p>
                    </div>
                  }
                </div>
              </div>

              <div>
                <h3 class="text-sm font-bold uppercase tracking-[0.14em] text-[#6B7D6C]">Suscripciones por estado</h3>
                <div class="mt-3 space-y-3">
                  @for (item of data.subscriptions.byStatus; track item.status) {
                    <div class="flex items-center justify-between rounded-xl border border-[#E8F2E2] bg-white px-4 py-3">
                      <div>
                        <p class="text-sm font-semibold text-[#1B5E20]">{{ item.status }}</p>
                        <p class="text-xs text-[#6B7D6C]">Estado general</p>
                      </div>
                      <p class="text-lg font-black text-[#2E7D32]">{{ item.total }}</p>
                    </div>
                  }
                </div>
              </div>
            </div>
          </div>

          <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-6 shadow-sm">
            <div class="flex items-center justify-between">
              <div>
                <h2 class="text-lg font-bold text-[#1B5E20]">Alertas</h2>
                <p class="text-sm text-[#6B7D6C]">Señales que requieren atención</p>
              </div>
              <span class="rounded-full bg-[#F1F8E9] px-3 py-1 text-xs font-bold text-[#2E7D32]">{{ data.alerts.length }}</span>
            </div>

            <div class="mt-4 space-y-3">
              @for (alert of data.alerts; track alert.id) {
                <div class="rounded-2xl border px-4 py-4"
                  [class.border-red-200]="severityLevel(alert.severity) === 'high'"
                  [class.bg-red-50]="severityLevel(alert.severity) === 'high'"
                  [class.border-amber-200]="severityLevel(alert.severity) === 'warn'"
                  [class.bg-amber-50]="severityLevel(alert.severity) === 'warn'"
                  [class.border-sky-200]="severityLevel(alert.severity) === 'info'"
                  [class.bg-sky-50]="severityLevel(alert.severity) === 'info'">
                  <div class="flex items-start justify-between gap-4">
                    <div>
                      <p class="text-sm font-bold text-[#1B5E20]">{{ alert.title }}</p>
                      <p class="mt-1 text-sm text-[#4F5D4F]">{{ alert.description }}</p>
                    </div>
                    <span class="rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-[0.14em]"
                      [class.bg-red-100]="severityLevel(alert.severity) === 'high'"
                      [class.text-red-700]="severityLevel(alert.severity) === 'high'"
                      [class.bg-amber-100]="severityLevel(alert.severity) === 'warn'"
                      [class.text-amber-800]="severityLevel(alert.severity) === 'warn'"
                      [class.bg-sky-100]="severityLevel(alert.severity) === 'info'"
                      [class.text-sky-700]="severityLevel(alert.severity) === 'info'">
                      {{ alert.severity }}
                    </span>
                  </div>
                </div>
              }

              @if (data.alerts.length === 0) {
                <div class="rounded-2xl border border-dashed border-[#C8E6C9] bg-[#FAFCF8] px-4 py-6 text-sm text-[#6B7D6C]">
                  No hay alertas activas.
                </div>
              }
            </div>
          </div>
        </section>

        <section class="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
          <div class="min-w-0 rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
            <div class="flex items-center justify-between gap-3 min-w-0">
              <div>
                <h2 class="text-lg font-bold text-[#1B5E20]">Tenants recientes</h2>
                <p class="text-sm text-[#6B7D6C]">Últimos registros creados en la plataforma</p>
              </div>
              <span class="shrink-0 text-xs font-semibold uppercase tracking-[0.14em] text-[#6B7D6C]">{{ data.tenants.recent.length }} items</span>
            </div>

            <div class="mt-4 overflow-x-auto rounded-2xl border border-[#E8F2E2]">
              <table class="min-w-[620px] w-full divide-y divide-[#E8F2E2]">
                <thead class="bg-[#F7FBF3]">
                  <tr class="text-left text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">
                    <th class="px-4 py-3">Tenant</th>
                    <th class="px-4 py-3">Slug</th>
                    <th class="px-4 py-3">Plan</th>
                    <th class="px-4 py-3">Estado</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-[#EEF5EA] bg-white">
                  @for (tenant of data.tenants.recent; track tenant.id) {
                    <tr>
                      <td class="px-4 py-3 min-w-0">
                        <p class="break-words font-semibold text-[#1B5E20]">{{ tenant.name }}</p>
                        <p class="break-words text-xs text-[#6B7D6C]">{{ tenant.schemaName }}</p>
                      </td>
                      <td class="px-4 py-3 text-sm text-[#4F5D4F] break-words">{{ tenant.slug }}</td>
                      <td class="px-4 py-3 text-sm text-[#4F5D4F] break-words">{{ tenant.planCode || 'Sin plan' }}</td>
                      <td class="px-4 py-3">
                        <span class="rounded-full px-3 py-1 text-xs font-bold uppercase tracking-[0.1em]"
                          [class.bg-[#E8F5E9]]="tenant.status === 'ACTIVE'"
                          [class.text-[#2E7D32]]="tenant.status === 'ACTIVE'"
                          [class.bg-red-100]="tenant.status !== 'ACTIVE'"
                          [class.text-red-700]="tenant.status !== 'ACTIVE'">
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
            <div class="min-w-0 rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
              <div class="flex items-center justify-between gap-3 min-w-0">
                <div>
                  <h2 class="text-lg font-bold text-[#1B5E20]">Actividad reciente</h2>
                  <p class="text-sm text-[#6B7D6C]">Eventos auditados más recientes</p>
                </div>
                <span class="shrink-0 text-xs font-semibold uppercase tracking-[0.14em] text-[#6B7D6C]">{{ recentActivityCards().length }} items</span>
              </div>

              <div class="mt-4 space-y-3">
                @for (activity of recentActivityCards(); track activity.id) {
                  <div class="rounded-2xl border border-[#E8F2E2] bg-[#FAFCF8] p-4">
                    <div class="flex flex-col gap-3 min-w-0">
                      <div class="flex items-start justify-between gap-3 min-w-0">
                        <div class="min-w-0 flex-1">
                          <p class="break-words text-sm font-bold text-[#1B5E20]">{{ activity.title }}</p>
                        </div>
                        <span class="shrink-0 rounded-full bg-[#E8F5E9] px-3 py-1 text-[11px] font-bold uppercase tracking-[0.12em] text-[#2E7D32]">
                          {{ activity.type }}
                        </span>
                      </div>

                      @if (activity.hasSummary && activity.summary; as summary) {
                        <div class="grid gap-2 sm:grid-cols-3">
                          @if (summary.name) {
                            <div class="rounded-xl border border-[#DDEED8] bg-white px-3 py-2">
                              <p class="text-[10px] font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Nombre</p>
                              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ summary.name }}</p>
                            </div>
                          }

                          @if (summary.slug) {
                            <div class="rounded-xl border border-[#DDEED8] bg-white px-3 py-2">
                              <p class="text-[10px] font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Slug</p>
                              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ summary.slug }}</p>
                            </div>
                          }

                          @if (summary.planCode) {
                            <div class="rounded-xl border border-[#DDEED8] bg-white px-3 py-2">
                              <p class="text-[10px] font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Plan</p>
                              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ summary.planCode }}</p>
                            </div>
                          }
                        </div>
                      }

                      <p class="text-xs text-[#6B7D6C]">{{ activity.actorName }} · {{ formatDate(activity.createdAt) }}</p>
                    </div>
                  </div>
                }
              </div>
            </div>

            <div class="min-w-0 rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
              <div class="flex items-center justify-between gap-3 min-w-0">
                <div>
                  <h2 class="text-lg font-bold text-[#1B5E20]">Insights</h2>
                  <p class="text-sm text-[#6B7D6C]">Lecturas rápidas del sistema</p>
                </div>
              </div>

              <div class="mt-4 space-y-3 min-w-0">
                @for (insight of data.insights; track insight.type) {
                  <div class="rounded-2xl border border-[#E8F2E2] bg-[#FAFCF8] px-4 py-4 min-w-0">
                    <p class="break-words text-sm font-bold text-[#1B5E20]">{{ insight.title }}</p>
                    <p class="mt-1 break-words text-sm text-[#4F5D4F]">{{ insight.description }}</p>
                  </div>
                }
              </div>
            </div>
          </div>
        </section>
      }

      @if (error()) {
        <div class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
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
