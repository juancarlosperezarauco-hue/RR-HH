import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { PlatformSubscriptionListUseCase } from '../../features/platform/application/platform-subscription-list.usecase';
import { PlatformSubscriptionAssignUseCase } from '../../features/platform/application/platform-subscription-assign.usecase';
import { PlatformSubscriptionGetByIdUseCase } from '../../features/platform/application/platform-subscription-get-by-id.usecase';
import { PlatformSubscriptionTableComponent } from '../../features/platform/ui/platform-subscription-table/platform-subscription-table.component';
import { PlatformSubscriptionAssignFormComponent } from '../../features/platform/ui/platform-subscription-assign-form/platform-subscription-assign-form.component';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { PlatformSubscription } from '../../entities/platform/api/platform.service';

type SubscriptionStatusFilter = 'all' | 'ACTIVE' | 'INACTIVE' | 'TRIAL';

@Component({
  selector: 'app-platform-subscriptions-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    PlatformSubscriptionTableComponent,
    PlatformSubscriptionAssignFormComponent,
    PlatformPaginationComponent
  ],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Plataforma superadmin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Gestión de suscripciones
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Administra las suscripciones activas, sus periodos de prueba y la relación entre tenants y planes.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Recargar
            </button>

            <button
              type="button"
              (click)="toggleAssignModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90">
              <lucide-icon name="plus" class="h-4 w-4"></lucide-icon>
              Asignar suscripción
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Suscripciones en página</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Cargadas en la vista actual</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Activas</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().active }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Con servicio vigente</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">En prueba</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().trial }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Dentro del periodo trial</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Filtradas</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ filteredSubscriptions().length }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Resultado visible en pantalla</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-1">
            <h2 class="text-lg font-bold text-accent-foreground">Filtros y búsqueda</h2>
            <p class="text-sm text-muted-foreground">Filtra por tenant, plan, slug o estado.</p>
          </div>

          <div class="flex flex-col gap-3 sm:flex-row">
            <label class="flex min-w-[240px] items-center gap-3 rounded-2xl border border-border bg-muted px-4 py-3">
              <span class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Buscar</span>
              <input
                type="text"
                [value]="searchQuery()"
                (input)="setSearchQuery($any($event.target).value)"
                placeholder="Tenant, slug o plan"
                class="w-full border-0 bg-transparent text-sm text-accent-foreground outline-none placeholder:text-muted-foreground" />
            </label>

            <div class="flex flex-wrap gap-2">
              @for (filter of statusFilters; track filter.value) {
                <button
                  type="button"
                  (click)="setStatusFilter(filter.value)"
                  class="cursor-pointer rounded-full px-4 py-2 text-sm font-semibold transition-colors"
                  [class.bg-primary]="statusFilter() === filter.value"
                  [class.text-primary-foreground]="statusFilter() === filter.value"
                  [class.bg-accent]="statusFilter() !== filter.value"
                  [class.text-primary]="statusFilter() !== filter.value">
                  {{ filter.label }}
                </button>
              }
            </div>
          </div>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-bold text-accent-foreground">Listado de suscripciones</h2>
            <p class="text-sm text-muted-foreground">
              {{ filteredSubscriptions().length }} resultado(s) de {{ stats().total }} suscripción(es)
            </p>
          </div>
        </div>

        <app-platform-subscription-table
          [subscriptions]="filteredSubscriptions()"
          [isLoading]="listUseCase.status() === 'loading'"
          (viewDetails)="viewDetails($event)">
        </app-platform-subscription-table>

        @if (listUseCase.page(); as page) {
          <div class="mt-4">
            <app-platform-pagination
              [currentPage]="page.number"
              [totalPages]="page.totalPages"
              [totalElements]="page.totalElements"
              [isLoading]="listUseCase.status() === 'loading'"
              (pageChange)="changePage($event)">
            </app-platform-pagination>
          </div>
        }

        @if (listUseCase.status() !== 'loading' && filteredSubscriptions().length === 0) {
          <div class="rounded-2xl border border-dashed border-border bg-muted px-4 py-8 text-sm text-muted-foreground">
            No hay suscripciones que coincidan con los filtros actuales.
          </div>
        }
      </section>

      @if (selectedSubscription(); as subscription) {
        <div class="app-modal-overlay" (click)="closeModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Detalle de la suscripción</h2>
                <p class="app-modal-subtitle">Información técnica y de estado del registro seleccionado.</p>
              </div>
              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full border border-border p-2 text-primary transition-colors hover:bg-accent">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="mt-6 grid gap-4 sm:grid-cols-2">
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Tenant</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">
                  {{ subscription.tenantName }}
                </p>
                <p class="mt-1 text-xs text-muted-foreground">{{ subscription.tenantSlug }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Plan</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">
                  {{ subscription.planName }}
                </p>
                <p class="mt-1 text-xs text-muted-foreground">{{ subscription.planCode }} - {{ subscription.planType }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Estado</p>
                <p class="mt-1 text-sm font-semibold" [class.text-primary]="subscription.status === 'ACTIVE'" [class.text-destructive]="subscription.status !== 'ACTIVE'">
                  {{ subscription.status }}
                </p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Trial</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ subscription.trial ? 'Sí' : 'No' }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Inicio</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ formatDate(subscription.startedAt) }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Expira</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ formatDate(subscription.expiresAt) }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Días restantes</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ subscription.remainingDays }} días</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Renovación actual</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">
                  {{ subscription.currentSubscription ? 'Sí' : 'No' }}
                </p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Máx. usuarios</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ subscription.maxUsers }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Máx. roles</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ subscription.maxRoles }}</p>
              </div>
            </div>

            <div class="app-modal-footer">
              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      }

      @if (showAssignModal()) {
        <div class="app-modal-overlay" (click)="closeAssignModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header border-b border-border pb-5">
              <div>
                <h2 class="app-modal-title">Asignar suscripción</h2>
                <p class="app-modal-subtitle">Selecciona un tenant y el plan que deseas asignar.</p>
              </div>
              <button
                type="button"
                (click)="closeAssignModal()"
                class="cursor-pointer rounded-full border border-border p-2 text-primary transition-colors hover:bg-accent">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="pt-6">
              <app-platform-subscription-assign-form
                [isLoading]="assignUseCase.status() === 'loading'"
                [error]="assignUseCase.error()"
                (submitForm)="onAssignSubscription($event)"
                (cancel)="closeAssignModal()">
              </app-platform-subscription-assign-form>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class PlatformSubscriptionsPageComponent implements OnInit {
  protected readonly listUseCase = inject(PlatformSubscriptionListUseCase);
  protected readonly assignUseCase = inject(PlatformSubscriptionAssignUseCase);
  protected readonly detailUseCase = inject(PlatformSubscriptionGetByIdUseCase);
  private readonly toastService = inject(ToastService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly pageSize = 20;

  readonly searchQuery = signal('');
  readonly statusFilter = signal<SubscriptionStatusFilter>('all');
  readonly showAssignModal = signal(false);
  readonly selectedSubscription = signal<PlatformSubscription | null>(null);

  readonly statusFilters: Array<{ label: string; value: SubscriptionStatusFilter }> = [
    { label: 'Todas', value: 'all' },
    { label: 'Activas', value: 'ACTIVE' },
    { label: 'Inactivas', value: 'INACTIVE' },
    { label: 'Trial', value: 'TRIAL' }
  ];

  readonly stats = computed(() => {
    const subscriptions = this.listUseCase.subscriptions();
    const active = subscriptions.filter((item) => item.status === 'ACTIVE').length;
    const trial = subscriptions.filter((item) => item.trial).length;
    return {
      total: subscriptions.length,
      active,
      trial
    };
  });

  readonly filteredSubscriptions = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const statusFilter = this.statusFilter();

    return [...this.listUseCase.subscriptions()]
      .filter((subscription) => {
        const matchesQuery =
          !query ||
          subscription.tenantName.toLowerCase().includes(query) ||
          subscription.tenantSlug.toLowerCase().includes(query) ||
          subscription.planName.toLowerCase().includes(query) ||
          subscription.planCode.toLowerCase().includes(query);

        const matchesStatus = this.matchesStatusFilter(subscription, statusFilter);
        return matchesQuery && matchesStatus;
      })
      .sort((left, right) => left.tenantName.localeCompare(right.tenantName));
  });

  ngOnInit(): void {
    void this.reload();
  }

  async reload(page = this.listUseCase.currentPage()): Promise<void> {
    await this.listUseCase.loadSubscriptions(page, this.pageSize);
    this.cdr.detectChanges();
  }

  async changePage(page: number): Promise<void> {
    await this.reload(page);
  }

  toggleAssignModal(): void {
    this.showAssignModal.update((current) => !current);
  }

  closeAssignModal(): void {
    this.showAssignModal.set(false);
    this.assignUseCase.resetState();
  }

  setSearchQuery(value: string): void {
    this.searchQuery.set(value);
  }

  setStatusFilter(value: SubscriptionStatusFilter): void {
    this.statusFilter.set(value);
  }

  async onAssignSubscription(data: { tenantId: string; planCode: string; overrideTrialDays?: number }): Promise<void> {
    const success = await this.assignUseCase.assignSubscription({
      tenantId: data.tenantId,
      planCode: data.planCode,
      overrideTrialDays: data.overrideTrialDays
    });

    if (success) {
      this.toastService.success('Suscripción asignada exitosamente');
      this.closeAssignModal();
      await this.reload();
      return;
    }

    this.toastService.error(this.assignUseCase.error() || 'Error al asignar suscripción');
  }

  async viewDetails(id: string): Promise<void> {
    await this.detailUseCase.loadSubscription(id);
    this.selectedSubscription.set(this.detailUseCase.subscription() ?? null);
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.selectedSubscription.set(null);
    this.detailUseCase.resetState();
    this.cdr.detectChanges();
  }

  protected formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'N/A';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleString('es-BO', {
      dateStyle: 'medium',
      timeStyle: 'short'
    });
  }

  private matchesStatusFilter(subscription: PlatformSubscription, filter: SubscriptionStatusFilter): boolean {
    if (filter === 'all') {
      return true;
    }

    if (filter === 'TRIAL') {
      return subscription.trial;
    }

    return subscription.status === filter;
  }
}
