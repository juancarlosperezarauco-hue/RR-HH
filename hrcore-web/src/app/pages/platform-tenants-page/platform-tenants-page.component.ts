import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { ConfirmModalService } from '../../shared/ui/confirm-modal';
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
      <!-- Encabezado de la página -->
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Plataforma Superadministrador
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Administración de Organizaciones
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Consulta, filtra y controla el estado operativo (activación y suspensión lógica) de todas las organizaciones (tenants) registradas en HRCore SaaS sin afectar sus datos históricos.
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
              (click)="toggleCreateForm()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90">
              <lucide-icon name="plus" class="h-4 w-4"></lucide-icon>
              {{ showCreateForm() ? 'Cerrar formulario' : 'Nueva organización' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Estadísticas de Organizaciones -->
      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Total registradas</p>
          <p class="mt-3 text-3xl font-black text-accent-foreground">{{ tenantStats().total }}</p>
          <p class="mt-1 text-xs text-muted-foreground">Organizaciones en el directorio</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-muted-foreground">Activas</p>
            <span class="h-2 w-2 rounded-full bg-emerald-500"></span>
          </div>
          <p class="mt-3 text-3xl font-black text-emerald-600 dark:text-emerald-400">{{ tenantStats().active }}</p>
          <p class="mt-1 text-xs text-muted-foreground">En estado operativo normal</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-muted-foreground">Suspendidas / Inactivas</p>
            <span class="h-2 w-2 rounded-full bg-destructive"></span>
          </div>
          <p class="mt-3 text-3xl font-black text-destructive">{{ tenantStats().inactive }}</p>
          <p class="mt-1 text-xs text-muted-foreground">Acceso bloqueado (datos seguros)</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Visibles / Filtradas</p>
          <p class="mt-3 text-3xl font-black text-primary">{{ filteredTenants().length }}</p>
          <p class="mt-1 text-xs text-muted-foreground">Coinciden con los criterios</p>
        </div>
      </section>

      <!-- Panel de Filtros y Búsqueda -->
      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="space-y-4">
          <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 class="text-lg font-bold text-accent-foreground">Filtros y Búsqueda Avanzada</h2>
              <p class="text-xs text-muted-foreground">Filtra el listado por nombre, identificación fiscal (NIT), plan contratado y estado operativo.</p>
            </div>
            @if (hasActiveFilters()) {
              <button
                type="button"
                (click)="clearFilters()"
                class="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline cursor-pointer">
                <lucide-icon name="rotate-ccw" class="h-3 w-3"></lucide-icon>
                Restablecer filtros
              </button>
            }
          </div>

          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <!-- Búsqueda General -->
            <label class="flex items-center gap-2.5 rounded-2xl border border-border bg-muted/60 px-3.5 py-2.5">
              <lucide-icon name="search" class="h-4 w-4 text-muted-foreground shrink-0"></lucide-icon>
              <input
                type="text"
                [value]="searchQuery()"
                (input)="setSearchQuery($any($event.target).value)"
                placeholder="Nombre, razón social o slug..."
                class="w-full border-0 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground" />
            </label>

            <!-- Filtro por NIT -->
            <label class="flex items-center gap-2.5 rounded-2xl border border-border bg-muted/60 px-3.5 py-2.5">
              <lucide-icon name="file-text" class="h-4 w-4 text-muted-foreground shrink-0"></lucide-icon>
              <input
                type="text"
                [value]="nitQuery()"
                (input)="setNitQuery($any($event.target).value)"
                placeholder="Identificación fiscal (NIT)..."
                class="w-full border-0 bg-transparent text-sm text-foreground outline-none placeholder:text-muted-foreground" />
            </label>

            <!-- Filtro por Plan -->
            <div class="relative">
              <select
                [value]="planFilter()"
                (change)="setPlanFilter($any($event.target).value)"
                class="w-full appearance-none rounded-2xl border border-border bg-muted/60 px-3.5 py-2.5 text-sm text-foreground outline-none cursor-pointer">
                <option value="all">Todos los planes</option>
                @for (plan of planUseCase.plans(); track plan.id) {
                  <option [value]="plan.id">{{ plan.name }} ({{ plan.code }})</option>
                }
              </select>
              <lucide-icon name="chevron-down" class="pointer-events-none absolute right-3.5 top-3.5 h-4 w-4 text-muted-foreground"></lucide-icon>
            </div>

            <!-- Filtro por Estado Operativo -->
            <div class="flex items-center gap-1 rounded-2xl border border-border bg-muted/60 p-1">
              @for (filter of statusFilters; track filter.value) {
                <button
                  type="button"
                  (click)="setStatusFilter(filter.value)"
                  class="flex-1 rounded-xl py-1.5 text-xs font-semibold transition-colors cursor-pointer text-center"
                  [class.bg-primary]="statusFilter() === filter.value"
                  [class.text-primary-foreground]="statusFilter() === filter.value"
                  [class.text-muted-foreground]="statusFilter() !== filter.value"
                  [class.hover:text-foreground]="statusFilter() !== filter.value">
                  {{ filter.label }}
                </button>
              }
            </div>
          </div>
        </div>
      </section>

      <!-- Tabla de Listado de Organizaciones -->
      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-bold text-accent-foreground">Directorio de Organizaciones</h2>
            <p class="text-sm text-muted-foreground">
              Mostrando {{ filteredTenants().length }} de {{ tenantStats().total }} organización(es) registrada(s)
            </p>
          </div>
        </div>

        <app-platform-tenant-table
          [tenants]="filteredTenants()"
          [planNames]="planNameById()"
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
      </section>

      <!-- Modal de Detalle Completo de Organización -->
      @if (selectedTenant(); as tenant) {
        <div class="app-modal-overlay" (click)="closeModal()">
          <div class="relative w-full max-w-2xl rounded-[28px] bg-card border border-border shadow-2xl flex flex-col max-h-[88vh] overflow-hidden" (click)="$event.stopPropagation()">
            <!-- Modal Header Fijo -->
            <div class="flex items-center justify-between border-b border-border p-6 pb-4 shrink-0 bg-card">
              <div class="flex items-center gap-3">
                <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary font-black text-lg">
                  {{ (tenant.name || 'O').charAt(0).toUpperCase() }}
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <h2 class="text-xl font-bold text-accent-foreground">{{ tenant.name }}</h2>
                    <span [class]="tenant.active ? 'bg-emerald-500/10 text-emerald-600 border-emerald-500/20' : 'bg-destructive/10 text-destructive border-destructive/20'"
                          class="rounded-full border px-2.5 py-0.5 text-xs font-semibold">
                      {{ tenant.active ? 'Activo' : 'Desactivado' }}
                    </span>
                  </div>
                  <p class="text-xs text-muted-foreground">Espacio de trabajo: {{ tenant.slug }} · Esquema: {{ tenant.schemaName }}</p>
                </div>
              </div>

              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full border border-border p-2 text-foreground transition-colors hover:bg-accent">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <!-- Modal Body (Único contenedor con scroll vertical) -->
            <div class="flex-1 overflow-y-auto p-6 space-y-5 overscroll-contain">
              <!-- Sección: Datos Generales y Fiscales -->
              <div>
                <h3 class="text-xs font-bold uppercase tracking-wider text-primary mb-3">Datos Generales y Fiscales</h3>
                <div class="grid gap-3 sm:grid-cols-2">
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Nombre Comercial</p>
                    <p class="mt-1 text-sm font-bold text-foreground">{{ tenant.name }}</p>
                  </div>
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Razón Social</p>
                    <p class="mt-1 text-sm font-bold text-foreground">{{ tenant.legalName || 'No registrada' }}</p>
                  </div>
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Identificación Fiscal / NIT</p>
                    <p class="mt-1 text-sm font-mono font-bold text-foreground">{{ tenant.taxIdentifier || 'No registrado' }}</p>
                  </div>
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Esquema en PostgreSQL</p>
                    <p class="mt-1 text-sm font-mono text-muted-foreground">{{ tenant.schemaName }}</p>
                  </div>
                </div>
              </div>

              <!-- Sección: Contacto y Responsable -->
              <div>
                <h3 class="text-xs font-bold uppercase tracking-wider text-primary mb-3">Propietario Responsable y Contacto</h3>
                <div class="grid gap-3 sm:grid-cols-2">
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Propietario Responsable</p>
                    <p class="mt-1 text-sm font-bold text-foreground">{{ tenant.ownerName || 'Administrador principal' }}</p>
                    <p class="text-xs text-muted-foreground">{{ tenant.ownerEmail || tenant.contactEmail || 'Sin correo asociado' }}</p>
                  </div>
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Contacto Oficial</p>
                    <p class="mt-1 text-sm text-foreground">{{ tenant.contactEmail || 'Sin correo de contacto' }}</p>
                    @if (tenant.contactPhone) {
                      <p class="text-xs text-muted-foreground">Tel: {{ tenant.contactPhone }}</p>
                    }
                  </div>
                </div>
              </div>

              <!-- Sección: Plan, Suscripción y Estado Operativo -->
              <div>
                <h3 class="text-xs font-bold uppercase tracking-wider text-primary mb-3">Plan, Suscripción y Estado Operativo</h3>
                <div class="grid gap-3 sm:grid-cols-2">
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Plan Contratado Vigente</p>
                    <p class="mt-1 text-sm font-bold text-foreground">{{ resolvePlanName(tenant) }}</p>
                    @if (tenant.planCode) {
                      <span class="inline-block mt-1 text-[11px] rounded bg-muted px-1.5 py-0.5 font-mono text-muted-foreground">Código: {{ tenant.planCode }}</span>
                    }
                  </div>
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Estado de Suscripción</p>
                    <p class="mt-1 text-sm font-bold text-foreground">{{ tenant.subscriptionStatus || 'ACTIVA' }}</p>
                  </div>
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Fecha de Registro</p>
                    <p class="mt-1 text-sm text-foreground">{{ formatDate(tenant.createdAt) }}</p>
                  </div>
                  <div class="rounded-xl border border-border bg-muted/40 p-3.5">
                    <p class="text-xs font-semibold text-muted-foreground">Última Modificación</p>
                    <p class="mt-1 text-sm text-foreground">{{ formatDate(tenant.updatedAt) }}</p>
                  </div>
                </div>
              </div>

              <!-- Aviso de Integridad de Datos -->
              <div class="rounded-2xl border border-border bg-muted/30 p-4 text-xs text-muted-foreground leading-relaxed">
                <span class="font-bold text-foreground">Aviso sobre control operativo:</span> La activación o desactivación de una organización se realiza de forma lógica en la plataforma SaaS. Toda la información histórica, nóminas, turnos y registros almacenados en PostgreSQL se conservan intactos.
              </div>
            </div>

            <!-- Modal Footer Fijo con Acciones Directas -->
            <div class="border-t border-border p-6 pt-4 shrink-0 bg-card flex items-center justify-between">
              <div>
                @if (tenant.active) {
                  <button
                    type="button"
                    (click)="onDeactivate(tenant.id)"
                    class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-destructive/30 bg-destructive/10 px-4 py-2 text-sm font-semibold text-destructive transition-colors hover:bg-destructive hover:text-destructive-foreground">
                    <lucide-icon name="pause-circle" class="h-4 w-4"></lucide-icon>
                    Desactivar Organización
                  </button>
                } @else {
                  <button
                    type="button"
                    (click)="onActivate(tenant.id)"
                    class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-semibold text-emerald-600 transition-colors hover:bg-emerald-600 hover:text-white">
                    <lucide-icon name="play-circle" class="h-4 w-4"></lucide-icon>
                    Activar Organización
                  </button>
                }
              </div>

              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full bg-secondary px-5 py-2 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/90">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Modal de Creación -->
      @if (showCreateForm()) {
        <div class="app-modal-overlay" (click)="closeCreateModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header border-b border-border pb-5">
              <div>
                <h2 class="app-modal-title">Nueva Organización</h2>
                <p class="app-modal-subtitle">Registra una nueva organización y asigna su plan inicial.</p>
              </div>
              <button
                type="button"
                (click)="closeCreateModal()"
                class="cursor-pointer rounded-full border border-border p-2 text-primary transition-colors hover:bg-accent">
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
        <div class="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {{ createUseCase.error() }}
        </div>
      }

      @if (error()) {
        <div class="rounded-2xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {{ error() }}
        </div>
      }
    </div>
  `
})
export class PlatformTenantsPageComponent implements OnInit, OnDestroy {
  protected readonly listUseCase = inject(PlatformTenantListUseCase);
  protected readonly createUseCase = inject(PlatformTenantCreateUseCase);
  protected readonly activateUseCase = inject(PlatformTenantActivateUseCase);
  protected readonly deactivateUseCase = inject(PlatformTenantDeactivateUseCase);
  protected readonly planUseCase = inject(PlatformPlanListUseCase);
  private readonly toastService = inject(ToastService);
  private readonly confirmModalService = inject(ConfirmModalService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly pageSize = 50;

  readonly statusFilters: Array<{ label: string; value: TenantStatusFilter }> = [
    { label: 'Todos', value: 'all' },
    { label: 'Activos', value: 'ACTIVE' },
    { label: 'Inactivos', value: 'INACTIVE' }
  ];

  readonly searchQuery = signal('');
  readonly nitQuery = signal('');
  readonly planFilter = signal('all');
  readonly statusFilter = signal<TenantStatusFilter>('all');
  readonly showCreateForm = signal(false);
  readonly selectedTenant = signal<PlatformTenant | null>(null);
  readonly tenantsLoading = signal(false);

  readonly planOptions = computed<TenantPlanOption[]>(() => {
    return this.planUseCase.plans().map((plan: PlatformPlan) => ({
      id: plan.id,
      code: plan.code,
      name: plan.name,
      description: plan.description,
      maxUsers: plan.maxUsers,
      maxRoles: plan.maxRoles,
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

  readonly hasActiveFilters = computed(() => {
    return (
      this.searchQuery().trim() !== '' ||
      this.nitQuery().trim() !== '' ||
      this.planFilter() !== 'all' ||
      this.statusFilter() !== 'all'
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
    const nit = this.nitQuery().trim().toLowerCase();
    const plan = this.planFilter();
    const status = this.statusFilter();

    return [...this.listUseCase.tenants()]
      .filter((tenant) => {
        const matchesQuery =
          !query ||
          tenant.name.toLowerCase().includes(query) ||
          (tenant.legalName && tenant.legalName.toLowerCase().includes(query)) ||
          tenant.slug.toLowerCase().includes(query) ||
          tenant.schemaName.toLowerCase().includes(query) ||
          (tenant.taxIdentifier && tenant.taxIdentifier.toLowerCase().includes(query));

        const matchesNit =
          !nit ||
          (tenant.taxIdentifier && tenant.taxIdentifier.toLowerCase().includes(nit));

        const matchesPlan =
          plan === 'all' ||
          tenant.planId === plan ||
          tenant.planCode === plan ||
          (tenant.planName && tenant.planName.toLowerCase() === plan.toLowerCase());

        const matchesStatus =
          status === 'all' ? true : (status === 'ACTIVE' ? tenant.active : !tenant.active);

        return matchesQuery && matchesNit && matchesPlan && matchesStatus;
      })
      .sort((left, right) => {
        const leftDate = new Date(left.createdAt || 0).getTime();
        const rightDate = new Date(right.createdAt || 0).getTime();
        return rightDate - leftDate;
      });
  });

  readonly error = computed(
    () =>
      this.listUseCase.error() ||
      this.createUseCase.error() ||
      this.activateUseCase.error() ||
      this.deactivateUseCase.error() ||
      this.planUseCase.error()
  );

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

  setNitQuery(value: string): void {
    this.nitQuery.set(value);
  }

  setPlanFilter(value: string): void {
    this.planFilter.set(value);
  }

  setStatusFilter(value: TenantStatusFilter): void {
    this.statusFilter.set(value);
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.nitQuery.set('');
    this.planFilter.set('all');
    this.statusFilter.set('all');
  }

  async onCreateTenant(data: CreateTenantFormData): Promise<void> {
    const success = await this.createUseCase.createTenant(data);
    if (success) {
      this.toastService.success('Organización creada exitosamente');
      this.closeCreateModal();
      await this.reload();
      return;
    }

    this.toastService.error(this.createUseCase.error() || 'Error al crear la organización');
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
  }

  async onActivate(id: string): Promise<void> {
    const tenant = this.listUseCase.tenants().find((item) => item.id === id);
    if (!tenant) {
      return;
    }

    const confirmed = await this.confirmModalService.confirm({
      title: '¿Reactivar organización?',
      message: `¿Estás seguro de que deseas reactivar la organización "${tenant.name}"?`,
      details: 'Los usuarios de esta organización podrán reanudar inmediatamente el inicio de sesión y acceder al espacio de trabajo.',
      confirmText: 'Reactivar Organización',
      cancelText: 'Cancelar',
      variant: 'success'
    });

    if (!confirmed) {
      return;
    }

    const success = await this.activateUseCase.activateTenant(id);
    if (success) {
      this.toastService.success(`Organización "${tenant.name}" activada exitosamente`);
      this.activateUseCase.resetState();
      await this.reload();
      if (this.selectedTenant()?.id === id) {
        const refreshed = this.listUseCase.tenants().find((item) => item.id === id) ?? null;
        this.selectedTenant.set(refreshed);
      }
      return;
    }

    this.toastService.error(this.activateUseCase.error() || 'Error al activar la organización');
  }

  async onDeactivate(id: string): Promise<void> {
    const tenant = this.listUseCase.tenants().find((item) => item.id === id);
    if (!tenant) {
      return;
    }

    const confirmed = await this.confirmModalService.confirm({
      title: '¿Suspender organización?',
      message: `¿Estás seguro de que deseas suspender/desactivar la organización "${tenant.name}"?`,
      details: 'Los datos históricos y registros en PostgreSQL permanecerán 100% intactos, pero los usuarios no podrán iniciar sesión ni acceder al espacio de trabajo.',
      confirmText: 'Suspender Organización',
      cancelText: 'Cancelar',
      variant: 'danger'
    });

    if (!confirmed) {
      return;
    }

    const success = await this.deactivateUseCase.deactivateTenant(id);
    if (success) {
      this.toastService.success(`Organización "${tenant.name}" desactivada exitosamente`);
      this.deactivateUseCase.resetState();
      await this.reload();
      if (this.selectedTenant()?.id === id) {
        const refreshed = this.listUseCase.tenants().find((item) => item.id === id) ?? null;
        this.selectedTenant.set(refreshed);
      }
      return;
    }

    this.toastService.error(this.deactivateUseCase.error() || 'Error al desactivar la organización');
  }

  viewDetails(id: string): void {
    const tenant = this.listUseCase.tenants().find((item) => item.id === id) ?? null;
    this.selectedTenant.set(tenant);
    if (typeof document !== 'undefined') {
      document.body.style.overflow = tenant ? 'hidden' : '';
    }
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.selectedTenant.set(null);
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
    this.cdr.detectChanges();
  }

  resolvePlanName(tenant: PlatformTenant): string {
    if (tenant.planName) {
      return tenant.planName;
    }
    if (tenant.planCode) {
      return tenant.planCode;
    }
    if (tenant.planId && this.planNameById().has(tenant.planId)) {
      return this.planNameById().get(tenant.planId)!;
    }
    return tenant.planId ? 'Plan asignado' : 'Sin plan';
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
}
