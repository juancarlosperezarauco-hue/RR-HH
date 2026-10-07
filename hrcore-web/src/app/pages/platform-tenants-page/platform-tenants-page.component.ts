import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { PlatformTenantActivateUseCase } from '../../features/platform/application/platform-tenant-activate.usecase';
import { PlatformTenantCreateUseCase } from '../../features/platform/application/platform-tenant-create.usecase';
import { PlatformTenantDeactivateUseCase } from '../../features/platform/application/platform-tenant-deactivate.usecase';
import { PlatformTenantListUseCase } from '../../features/platform/application/platform-tenant-list.usecase';
import { PlatformPlanListUseCase } from '../../features/platform/application/platform-plan-list.usecase';
import { CreateTenantFormData, PlatformTenantFormComponent, TenantPlanOption } from '../../features/platform/ui/platform-tenant-form/platform-tenant-form.component';
import { PlatformTenantTableComponent } from '../../features/platform/ui/platform-tenant-table/platform-tenant-table.component';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { PlatformPlan, PlatformTenant } from '../../entities/platform/api/platform.service';

type TenantStatusFilter = 'all' | 'ACTIVE' | 'INACTIVE';

@Component({
  selector: 'app-platform-tenants-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    PlatformTenantTableComponent,
    PlatformTenantFormComponent,
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
                Gestión de tenants
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Crea, inspecciona y administra tenants con una vista centralizada de su estado y plan asignado.
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
              (click)="toggleCreateForm()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#2E7D32] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#256428]">
              <lucide-icon name="plus" class="h-4 w-4"></lucide-icon>
              {{ showCreateForm() ? 'Cerrar formulario' : 'Nuevo tenant' }}
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Tenants en página</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ tenantStats().total }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Cargados en la vista actual</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Activos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ tenantStats().active }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Disponibles para operación</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Inactivos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ tenantStats().inactive }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Suspendidos o detenidos</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Filtrados</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ filteredTenants().length }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Resultado visible en pantalla</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-1">
            <h2 class="text-lg font-bold text-[#1B5E20]">Filtros y búsqueda</h2>
            <p class="text-sm text-[#6B7D6C]">Filtra por nombre, slug, schema o estado.</p>
          </div>

          <div class="flex flex-col gap-3 sm:flex-row">
            <label class="flex min-w-[240px] items-center gap-3 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3">
              <span class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Buscar</span>
              <input
                type="text"
                [value]="searchQuery()"
                (input)="setSearchQuery($any($event.target).value)"
                placeholder="Nombre, slug o schema"
                class="w-full border-0 bg-transparent text-sm text-[#1B5E20] outline-none placeholder:text-[#9AA99A]" />
            </label>

            <div class="flex flex-wrap gap-2">
              @for (filter of statusFilters; track filter.value) {
                <button
                  type="button"
                  (click)="setStatusFilter(filter.value)"
                  class="cursor-pointer rounded-full px-4 py-2 text-sm font-semibold transition-colors"
                  [class.bg-[#2E7D32]]="statusFilter() === filter.value"
                  [class.text-white]="statusFilter() === filter.value"
                  [class.bg-[#F1F8E9]]="statusFilter() !== filter.value"
                  [class.text-[#2E7D32]]="statusFilter() !== filter.value">
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
            <h2 class="text-lg font-bold text-[#1B5E20]">Listado de tenants</h2>
            <p class="text-sm text-[#6B7D6C]">
              {{ filteredTenants().length }} resultado(s) de {{ tenantStats().total }} tenant(s)
            </p>
          </div>
        </div>

        <app-platform-tenant-table
          [tenants]="filteredTenants()"
          [isLoading]="tenantsLoading()"
          (viewDetails)="viewDetails($event)"
          (activate)="onActivate($event)"
          (deactivate)="onDeactivate($event)">
        </app-platform-tenant-table>

        @if (listUseCase.page(); as page) {
          <div class="mt-4">
            <app-platform-pagination
              [currentPage]="page.number"
              [totalPages]="page.totalPages"
              [totalElements]="page.totalElements"
              [isLoading]="tenantsLoading()"
              (pageChange)="changePage($event)">
            </app-platform-pagination>
          </div>
        }

        @if (!tenantsLoading() && filteredTenants().length === 0) {
          <div class="rounded-2xl border border-dashed border-[#C8E6C9] bg-[#FAFCF8] px-4 py-8 text-sm text-[#6B7D6C]">
            No hay tenants que coincidan con los filtros actuales.
          </div>
        }
      </section>

      @if (selectedTenant(); as tenant) {
        <div class="app-modal-overlay" (click)="closeModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Detalle del tenant</h2>
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
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Nombre</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ tenant.name }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Slug</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ tenant.slug }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Schema</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ tenant.schemaName }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Plan</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ planNameById().get(tenant.planId || '') }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Estado</p>
                <p class="mt-1 text-sm font-semibold" [class.text-[#2E7D32]]="tenant.active" [class.text-[#C62828]]="!tenant.active">
                  {{ tenant.status }}
                </p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Creado</p>
                <p class="mt-1 text-sm font-semibold text-[#1B5E20]">{{ formatDate(tenant.createdAt) }}</p>
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

      @if (showCreateForm()) {
        <div class="app-modal-overlay" (click)="closeCreateModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header border-b border-[#E8F2E2] pb-5">
              <div>
                <h2 class="app-modal-title">Nuevo tenant</h2>
                <p class="app-modal-subtitle">Completa los datos y selecciona un plan disponible.</p>
              </div>
              <button
                type="button"
                (click)="closeCreateModal()"
                class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="pt-6">
              <app-platform-tenant-form
                [isLoading]="createUseCase.status() === 'loading'"
                [error]="createUseCase.error()"
                [plans]="planOptions()"
                (submitForm)="onCreateTenant($event)"
                (cancel)="closeCreateModal()">
              </app-platform-tenant-form>
            </div>
          </div>
        </div>
      }

      @if (createUseCase.error()) {
        <div class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {{ createUseCase.error() }}
        </div>
      }

      @if (error()) {
        <div class="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {{ error() }}
        </div>
      }
    </div>
  `
})
export class PlatformTenantsPageComponent implements OnInit {
  protected readonly listUseCase = inject(PlatformTenantListUseCase);
  protected readonly createUseCase = inject(PlatformTenantCreateUseCase);
  protected readonly activateUseCase = inject(PlatformTenantActivateUseCase);
  protected readonly deactivateUseCase = inject(PlatformTenantDeactivateUseCase);
  protected readonly planUseCase = inject(PlatformPlanListUseCase);
  private readonly toastService = inject(ToastService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly pageSize = 20;

  readonly statusFilters: Array<{ label: string; value: TenantStatusFilter }> = [
    { label: 'Todos', value: 'all' },
    { label: 'Activos', value: 'ACTIVE' },
    { label: 'Inactivos', value: 'INACTIVE' }
  ];

  readonly searchQuery = signal('');
  readonly statusFilter = signal<TenantStatusFilter>('all');
  readonly showCreateForm = signal(false);
  readonly selectedTenant = signal<PlatformTenant | null>(null);
  readonly tenantsLoading = signal(false);

  readonly planOptions = computed<TenantPlanOption[]>(() => {
    return this.planUseCase.plans().map((plan: PlatformPlan) => ({
        id: plan.id,                    // ✅ Agregar id
        code: plan.code,
        name: plan.name,
        description: plan.description,   // ✅ Agregar description
        maxUsers: plan.maxUsers,         // ✅ Agregar maxUsers
        maxRoles: plan.maxRoles,         // ✅ Agregar maxRoles
        planType: plan.planType,
        active: plan.active,
        trialDays: plan.trialDays
    }));
  });

  readonly planNameById = computed(() => {
    return new Map(
      this.planUseCase.plans().map((plan) => [plan.id, plan.name] as const)
    );
  });

  readonly tenantStats = computed(() => {
    const tenants = this.listUseCase.tenants();
    const active = tenants.filter((tenant) => tenant.active).length;
    const inactive = tenants.length - active;
    return {
      total: tenants.length,
      active,
      inactive
    };
  });

  readonly filteredTenants = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    return [...this.listUseCase.tenants()]
      .filter((tenant) => {
        const matchesQuery =
          !query ||
          tenant.name.toLowerCase().includes(query) ||
          tenant.slug.toLowerCase().includes(query) ||
          tenant.schemaName.toLowerCase().includes(query);

        const matchesStatus = status === 'all' ? true : (status === 'ACTIVE' ? tenant.active : !tenant.active);

        return matchesQuery && matchesStatus;
      })
      .sort((left, right) => {
        const leftDate = new Date(left.createdAt || 0).getTime();
        const rightDate = new Date(right.createdAt || 0).getTime();
        return rightDate - leftDate;
      });
  });

  readonly error = computed(() => this.listUseCase.error() || this.createUseCase.error() || this.activateUseCase.error() || this.deactivateUseCase.error() || this.planUseCase.error());

  ngOnInit(): void {
    void this.reload();
  }

  async reload(page = this.listUseCase.currentPage()): Promise<void> {
    this.tenantsLoading.set(true);

    try {
      await this.listUseCase.loadTenants(page, this.pageSize);
    } finally {
      this.tenantsLoading.set(false);
      void this.planUseCase.loadPlans(0, 200);
      this.cdr.detectChanges();
    }
  }

  async changePage(page: number): Promise<void> {
    await this.reload(page);
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((current) => !current);
  }

  closeCreateModal(): void {
    this.showCreateForm.set(false);
    this.createUseCase.resetState();
  }

  setSearchQuery(value: string): void {
    this.searchQuery.set(value);
  }

  setStatusFilter(value: TenantStatusFilter): void {
    this.statusFilter.set(value);
  }

  async onCreateTenant(data: CreateTenantFormData): Promise<void> {
    const success = await this.createUseCase.createTenant(data);
    if (success) {
      this.toastService.success('Tenant creado exitosamente');
      this.closeCreateModal();
      await this.reload();
      return;
    }

    this.toastService.error(this.createUseCase.error() || 'Error al crear tenant');
    this.cdr.detectChanges();
  }

  async onActivate(id: string): Promise<void> {
    const tenant = this.listUseCase.tenants().find((item) => item.id === id);
    if (!tenant) {
      return;
    }

    if (!confirm(`¿Estás seguro de que deseas activar el tenant "${tenant.name}"?`)) {
      return;
    }

    const success = await this.activateUseCase.activateTenant(id);
    if (success) {
      this.toastService.success(`Tenant "${tenant.name}" activado exitosamente`);
      this.activateUseCase.resetState();
      await this.reload();
      return;
    }

    this.toastService.error(this.activateUseCase.error() || 'Error al activar tenant');
  }

  async onDeactivate(id: string): Promise<void> {
    const tenant = this.listUseCase.tenants().find((item) => item.id === id);
    if (!tenant) {
      return;
    }

    if (!confirm(`¿Estás seguro de que deseas desactivar el tenant "${tenant.name}"?\n\nLos usuarios no podrán acceder hasta que sea reactivado.`)) {
      return;
    }

    const success = await this.deactivateUseCase.deactivateTenant(id);
    if (success) {
      this.toastService.success(`Tenant "${tenant.name}" desactivado exitosamente`);
      this.deactivateUseCase.resetState();
      await this.reload();
      return;
    }

    this.toastService.error(this.deactivateUseCase.error() || 'Error al desactivar tenant');
  }

  viewDetails(id: string): void {
    const tenant = this.listUseCase.tenants().find((item) => item.id === id) ?? null;
    this.selectedTenant.set(tenant);
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.selectedTenant.set(null);
    this.cdr.detectChanges();
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

  planLabel(planId: string | null | undefined): string {
    if (!planId) {
      return 'Sin plan';
    }

    return this.planNameById().get(planId) || planId;
  }
}
