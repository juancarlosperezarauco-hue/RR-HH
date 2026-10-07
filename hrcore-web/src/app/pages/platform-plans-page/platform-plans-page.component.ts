import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { PlatformPlanListUseCase } from '../../features/platform/application/platform-plan-list.usecase';
import { PlatformPlanCreateUseCase } from '../../features/platform/application/platform-plan-create.usecase';
import { PlatformPlanActivateUseCase } from '../../features/platform/application/platform-plan-activate.usecase';
import { PlatformPlanDeactivateUseCase } from '../../features/platform/application/platform-plan-deactivate.usecase';
import { PlatformPlanFormComponent } from '../../features/platform/ui/platform-plan-form/platform-plan-form.component';
import { PlatformPlanTableComponent } from '../../features/platform/ui/platform-plan-table/platform-plan-table.component';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { PlatformPlan } from '../../entities/platform/api/platform.service';

type PlanTypeFilter = 'all' | 'DEMO' | 'PAID';

@Component({
  selector: 'app-platform-plans-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    PlatformPlanTableComponent,
    PlatformPlanFormComponent,
    PlatformPaginationComponent
  ],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              Plataforma superadmin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Gestión de planes
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Administra los planes disponibles, sus límites y su estado de publicación.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Recargar
            </button>

            <button
              type="button"
              (click)="toggleCreateModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#2E7D32] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#256428]">
              <lucide-icon name="plus" class="h-4 w-4"></lucide-icon>
              Nuevo plan
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Planes en página</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Cargados en la vista actual</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Activos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().active }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Disponibles para asignación</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Inactivos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().inactive }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Suspendidos o deshabilitados</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Filtrados</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ filteredPlans().length }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Resultado visible</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-1">
            <h2 class="text-lg font-bold text-[#1B5E20]">Filtros y búsqueda</h2>
            <p class="text-sm text-[#6B7D6C]">Filtra por código, nombre o tipo de plan.</p>
          </div>

          <div class="flex flex-col gap-3 sm:flex-row">
            <label class="flex min-w-[240px] items-center gap-3 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3">
              <span class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Buscar</span>
              <input
                type="text"
                [value]="searchQuery()"
                (input)="setSearchQuery($any($event.target).value)"
                placeholder="Código, nombre o descripción"
                class="w-full border-0 bg-transparent text-sm text-[#1B5E20] outline-none placeholder:text-[#9AA99A]" />
            </label>

            <div class="flex flex-wrap gap-2">
              @for (filter of typeFilters; track filter.value) {
                <button
                  type="button"
                  (click)="setTypeFilter(filter.value)"
                  class="cursor-pointer rounded-full px-4 py-2 text-sm font-semibold transition-colors"
                  [class.bg-[#2E7D32]]="typeFilter() === filter.value"
                  [class.text-white]="typeFilter() === filter.value"
                  [class.bg-[#F1F8E9]]="typeFilter() !== filter.value"
                  [class.text-[#2E7D32]]="typeFilter() !== filter.value">
                  {{ filter.label }}
                </button>
              }
            </div>
          </div>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-bold text-[#1B5E20]">Listado de planes</h2>
            <p class="text-sm text-[#6B7D6C]">
              {{ filteredPlans().length }} resultado(s) de {{ stats().total }} plan(es)
            </p>
          </div>
        </div>

        <app-platform-plan-table
          [plans]="filteredPlans()"
          [isLoading]="listUseCase.status() === 'loading'"
          (viewDetails)="viewDetails($event)"
          (activate)="onActivate($event)"
          (deactivate)="onDeactivate($event)">
        </app-platform-plan-table>

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

        @if (listUseCase.status() !== 'loading' && filteredPlans().length === 0) {
          <div class="rounded-2xl border border-dashed border-[#C8E6C9] bg-[#FAFCF8] px-4 py-8 text-sm text-[#6B7D6C]">
            No hay planes que coincidan con los filtros actuales.
          </div>
        }
      </section>

      @if (selectedPlan(); as plan) {
        <div class="app-modal-overlay" (click)="closeModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Detalle del plan</h2>
                <p class="app-modal-subtitle">Información técnica y de estado del registro seleccionado.</p>
              </div>
              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="mt-6 grid gap-4 sm:grid-cols-2">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Código</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ plan.code }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Nombre</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ plan.name }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Tipo</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ plan.planType }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Estado</p>
                <p class="mt-1 text-sm font-semibold" [class.text-[#2E7D32]]="plan.active" [class.text-[#C62828]]="!plan.active">
                  {{ plan.active ? 'ACTIVO' : 'INACTIVO' }}
                </p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Máx. usuarios</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ plan.maxUsers }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Máx. roles</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ plan.maxRoles }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Días de prueba</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ plan.trialDays ?? 'N/A' }}</p>
              </div>
            </div>

            <div class="app-modal-footer">
              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full bg-[#2E7D32] px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#256428]">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      }

      @if (showCreateModal()) {
        <div class="app-modal-overlay" (click)="closeCreateModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header border-b border-[#E8F2E2] pb-5">
              <div>
                <h2 class="app-modal-title">Nuevo plan</h2>
                <p class="app-modal-subtitle">Define un plan y sus límites antes de publicarlo.</p>
              </div>
              <button
                type="button"
                (click)="closeCreateModal()"
                class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="pt-6">
              <app-platform-plan-form
                [isLoading]="createUseCase.status() === 'loading'"
                [error]="createUseCase.error()"
                (submitForm)="onCreatePlan($event)"
                (cancel)="closeCreateModal()">
              </app-platform-plan-form>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class PlatformPlansPageComponent implements OnInit {
  protected readonly listUseCase = inject(PlatformPlanListUseCase);
  protected readonly createUseCase = inject(PlatformPlanCreateUseCase);
  protected readonly activateUseCase = inject(PlatformPlanActivateUseCase);
  protected readonly deactivateUseCase = inject(PlatformPlanDeactivateUseCase);
  private readonly toastService = inject(ToastService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly pageSize = 20;

  readonly searchQuery = signal('');
  readonly typeFilter = signal<PlanTypeFilter>('all');
  readonly showCreateModal = signal(false);
  readonly selectedPlan = signal<PlatformPlan | null>(null);

  readonly typeFilters: Array<{ label: string; value: PlanTypeFilter }> = [
    { label: 'Todos', value: 'all' },
    { label: 'Demo', value: 'DEMO' },
    { label: 'Pago', value: 'PAID' }
  ];

  readonly stats = computed(() => {
    const plans = this.listUseCase.plans();
    const active = plans.filter((plan) => plan.active).length;
    return {
      total: plans.length,
      active,
      inactive: plans.length - active
    };
  });

  readonly filteredPlans = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const typeFilter = this.typeFilter();

    return [...this.listUseCase.plans()]
      .filter((plan) => {
        const matchesQuery =
          !query ||
          plan.code.toLowerCase().includes(query) ||
          plan.name.toLowerCase().includes(query) ||
          plan.description.toLowerCase().includes(query);

        const matchesType = typeFilter === 'all' ? true : plan.planType === typeFilter;
        return matchesQuery && matchesType;
      })
      .sort((left, right) => left.code.localeCompare(right.code));
  });

  ngOnInit(): void {
    void this.reload();
  }

  async reload(page = this.listUseCase.currentPage()): Promise<void> {
    await this.listUseCase.loadPlans(page, this.pageSize);
    this.cdr.detectChanges();
  }

  async changePage(page: number): Promise<void> {
    await this.reload(page);
  }

  toggleCreateModal(): void {
    this.showCreateModal.update((current) => !current);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.createUseCase.resetState();
  }

  setSearchQuery(value: string): void {
    this.searchQuery.set(value);
  }

  setTypeFilter(value: PlanTypeFilter): void {
    this.typeFilter.set(value);
  }

  async onCreatePlan(data: any): Promise<void> {
    const success = await this.createUseCase.createPlan(data);
    if (success) {
      this.toastService.success('Plan creado exitosamente');
      this.closeCreateModal();
      await this.reload();
      return;
    }

    this.toastService.error(this.createUseCase.error() || 'Error al crear plan');
  }

  async onActivate(id: string): Promise<void> {
    const plan = this.listUseCase.plans().find((item) => item.id === id);
    if (!plan) {
      return;
    }

    if (!confirm(`¿Estás seguro de que deseas activar el plan "${plan.name}"?`)) {
      return;
    }

    const success = await this.activateUseCase.activatePlan(id);
    if (success) {
      this.toastService.success(`Plan "${plan.name}" activado exitosamente`);
      this.activateUseCase.resetState();
      await this.reload();
      return;
    }

    this.toastService.error(this.activateUseCase.error() || 'Error al activar plan');
  }

  async onDeactivate(id: string): Promise<void> {
    const plan = this.listUseCase.plans().find((item) => item.id === id);
    if (!plan) {
      return;
    }

    if (!confirm(`¿Estás seguro de que deseas desactivar el plan "${plan.name}"?`)) {
      return;
    }

    const success = await this.deactivateUseCase.deactivatePlan(id);
    if (success) {
      this.toastService.success(`Plan "${plan.name}" desactivado exitosamente`);
      this.deactivateUseCase.resetState();
      await this.reload();
      return;
    }

    this.toastService.error(this.deactivateUseCase.error() || 'Error al desactivar plan');
  }

  viewDetails(id: string): void {
    const plan = this.listUseCase.plans().find((item) => item.id === id) ?? null;
    this.selectedPlan.set(plan);
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.selectedPlan.set(null);
    this.cdr.detectChanges();
  }
}
