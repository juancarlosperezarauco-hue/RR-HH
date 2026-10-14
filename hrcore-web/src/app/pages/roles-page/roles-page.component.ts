import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { PermissionListUseCase, RoleFormComponent, RoleFormUseCase, RoleListUseCase, RoleTableComponent } from '../../features/role-management';
import { CreateTenantRoleRequest, TenantRoleResponse, UpdateTenantRoleRequest } from '../../entities/access';
import { PermissionService } from '../../shared/lib/auth/permission.service';

type RoleStatusFilter = 'all' | 'active' | 'inactive';

@Component({
  selector: 'app-roles-page',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, RoleTableComponent, RoleFormComponent],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8 relative">
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Accesos y control
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Roles del tenant
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Define qué permisos agrupan tus colaboradores y cómo se distribuyen dentro del tenant.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="refresh-ccw" [size]="16"></lucide-icon>
              Recargar
            </button>

            <button
              *ngIf="canManageRoles"
              type="button"
              (click)="openRoleModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 shadow-sm">
              <lucide-icon name="plus" [size]="16"></lucide-icon>
              Nuevo rol
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Roles</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Cargados en la página</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Activos</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().active }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Disponibles para asignar</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Inactivos</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().inactive }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Suspendidos o deshabilitados</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Permisos asignados</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().permissionLinks }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Suma total por rol</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Filtrados</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().filtered }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Resultados visibles</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-1">
            <h2 class="text-lg font-bold text-accent-foreground">Filtros</h2>
            <p class="text-sm text-muted-foreground">Filtra por nombre, descripción o estado.</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <label class="flex min-w-[260px] items-center gap-3 rounded-2xl border border-border bg-muted px-4 py-3">
              <span class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Buscar</span>
              <input
                type="text"
                [ngModel]="searchQuery()"
                (ngModelChange)="searchQuery.set($event)"
                placeholder="Nombre, descripción o permiso"
                class="w-full border-0 bg-transparent text-sm text-accent-foreground outline-none placeholder:text-muted-foreground" />
            </label>

            <select
              [ngModel]="statusFilter()"
              (ngModelChange)="statusFilter.set($event)"
              class="h-12 rounded-2xl border border-border bg-muted px-4 text-sm text-accent-foreground outline-none transition-colors focus:border-primary focus:bg-card">
              <option value="all">Todos los estados</option>
              <option value="active">Activos</option>
              <option value="inactive">Inactivos</option>
            </select>
          </div>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-bold text-accent-foreground">Listado de roles</h2>
            <p class="text-sm text-muted-foreground">
              {{ filteredRoles().length }} resultado(s) de {{ roleListUseCase.data().length }} rol(es)
            </p>
          </div>
          <div class="rounded-full border border-border bg-muted px-3 py-1 text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">
            {{ canReadPermissionCatalog ? permissionListUseCase.data().length : 'Catálogo restringido' }}
          </div>
        </div>

        @if (roleListUseCase.status() === 'loading' && roleListUseCase.data().length === 0) {
          <div class="flex flex-col items-center justify-center gap-4 p-12 text-muted-foreground">
            <svg class="h-8 w-8 animate-spin text-primary" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p>Cargando roles...</p>
          </div>
        }

        @if (roleListUseCase.status() === 'error') {
          <div class="rounded-2xl border border-destructive/20 bg-destructive/5 p-6 flex items-start gap-4">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mt-0.5 text-destructive">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" x2="12" y1="8" y2="12"></line>
              <line x1="12" x2="12.01" y1="16" y2="16"></line>
            </svg>
            <div>
              <h3 class="font-semibold text-destructive">Error al cargar roles</h3>
              <p class="mt-1 text-sm text-destructive/80">{{ roleListUseCase.error() }}</p>
              <button (click)="reload()" class="mt-3 cursor-pointer text-sm font-semibold text-destructive hover:underline">Intentar nuevamente</button>
            </div>
          </div>
        }

        @if (roleListUseCase.status() === 'success' || (roleListUseCase.status() === 'loading' && roleListUseCase.data().length > 0)) {
          <app-role-table
            [roles]="filteredRoles()"
            [showActions]="canManageRoles"
            (edit)="openRoleModal($event)"
            (toggleStatus)="handleToggleStatus($event.id, $event.currentStatus)">
          </app-role-table>
        }

        @if (roleListUseCase.status() !== 'loading' && filteredRoles().length === 0) {
          <div class="rounded-2xl border border-dashed border-border bg-muted px-4 py-8 text-sm text-muted-foreground">
            No hay roles que coincidan con los filtros actuales.
          </div>
        }
      </section>

      @if (showSuccessToast()) {
        <div class="fixed bottom-4 right-4 z-50 flex items-center gap-2 rounded-full bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-lg">
          <lucide-icon name="check-circle" [size]="16"></lucide-icon>
          Operación exitosa
        </div>
      }

      @if (isModalOpen()) {
        <div class="app-modal-overlay">
          <div class="w-full max-w-5xl rounded-[28px] border border-border bg-card shadow-2xl overflow-hidden flex h-[calc(100vh-2rem)] flex-col">
            <div class="app-modal-header border-b border-border px-6 py-5">
              <div>
                <h2 class="app-modal-title">
                  {{ roleToEdit() ? 'Editar rol' : 'Nuevo rol' }}
                </h2>
                <p class="app-modal-subtitle">
                  {{ roleToEdit() ? 'Ajusta nombre y permisos del rol.' : 'Crea un rol y define su alcance desde el inicio.' }}
                </p>
              </div>
              <button
                type="button"
                (click)="closeRoleModal()"
                class="cursor-pointer rounded-full border border-border p-2 text-primary transition-colors hover:bg-accent"
                [disabled]="roleFormUseCase.status() === 'loading'">
                <lucide-icon name="x" [size]="20"></lucide-icon>
              </button>
            </div>

            <div class="flex-1 overflow-hidden">
              @if (canReadPermissionCatalog && permissionListUseCase.status() === 'loading') {
                <div class="flex h-full items-center justify-center p-12 text-muted-foreground">
                  Cargando permisos disponibles...
                </div>
              } @else if (canReadPermissionCatalog && permissionListUseCase.status() === 'error') {
                <div class="m-6 rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">
                  No se pudieron cargar los permisos: {{ permissionListUseCase.error() }}
                </div>
              } @else if (!canReadPermissionCatalog) {
                <div class="m-6 rounded-2xl border border-dashed border-border bg-muted p-6 text-sm text-muted-foreground">
                  No tienes acceso al catálogo de permisos. Puedes ver los roles, pero no crear ni editar su asignación.
                </div>
              } @else {
                <app-role-form
                  class="h-full"
                  [status]="roleFormUseCase.status()"
                  [roleToEdit]="roleToEdit()"
                  [permissions]="permissionListUseCase.data()"
                  (submitCreate)="handleCreateRole($event)"
                  (submitUpdate)="handleUpdateRole($event.id, $event.request)"
                  (cancel)="closeRoleModal()">
                </app-role-form>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class RolesPageComponent implements OnInit {
  public readonly roleListUseCase = inject(RoleListUseCase);
  public readonly permissionListUseCase = inject(PermissionListUseCase);
  public readonly roleFormUseCase = inject(RoleFormUseCase);
  private readonly permissionService = inject(PermissionService);

  public readonly searchQuery = signal('');
  public readonly statusFilter = signal<RoleStatusFilter>('all');
  public readonly isModalOpen = signal(false);
  public readonly roleToEdit = signal<TenantRoleResponse | null>(null);
  public readonly showSuccessToast = signal(false);

  get canReadPermissionCatalog(): boolean {
    return this.permissionService.hasPermission('access.permissions.read');
  }

  get canManageRoles(): boolean {
    return this.canReadPermissionCatalog && this.permissionService.hasAnyPermission(
      'access.roles.create',
      'access.roles.update',
      'access.roles.activate',
      'access.roles.deactivate'
    );
  }

  readonly filteredRoles = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const statusFilter = this.statusFilter();

    return this.roleListUseCase.data().filter(role => {
      const statusMatches =
        statusFilter === 'all' ||
        (statusFilter === 'active' && role.active) ||
        (statusFilter === 'inactive' && !role.active);

      const searchableText = [
        role.name,
        role.description,
        role.permissionCodes.join(' ')
      ].join(' ').toLowerCase();

      const queryMatches = query.length === 0 || searchableText.includes(query);

      return statusMatches && queryMatches;
    });
  });

  readonly stats = computed(() => {
    const roles = this.roleListUseCase.data();
    const active = roles.filter(role => role.active).length;
    const permissionLinks = roles.reduce((acc, role) => acc + role.permissionCodes.length, 0);

    return {
      total: roles.length,
      active,
      inactive: roles.length - active,
      permissionLinks,
      filtered: this.filteredRoles().length
    };
  });

  constructor() {
    effect(() => {
      if (this.roleFormUseCase.status() === 'success') {
        this.closeRoleModal();
        this.roleFormUseCase.resetState();
        this.roleListUseCase.reloadRoles();
        this.triggerSuccessToast();
      }
    });
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.roleListUseCase.loadRoles();
    if (this.canReadPermissionCatalog) {
      this.permissionListUseCase.loadPermissions();
    }
  }

  openRoleModal(role: TenantRoleResponse | null = null): void {
    if (!this.canManageRoles) {
      return;
    }

    this.roleFormUseCase.resetState();
    this.roleToEdit.set(role);
    if (this.canReadPermissionCatalog) {
      this.permissionListUseCase.loadPermissions();
    }
    this.isModalOpen.set(true);
  }

  closeRoleModal(): void {
    if (this.roleFormUseCase.status() === 'loading') return;
    this.isModalOpen.set(false);
    setTimeout(() => this.roleToEdit.set(null), 300);
  }

  handleCreateRole(request: CreateTenantRoleRequest): void {
    this.roleFormUseCase.createRole(request);
  }

  handleUpdateRole(id: string, request: UpdateTenantRoleRequest): void {
    this.roleFormUseCase.updateRole(id, request);
  }

  async handleToggleStatus(id: string, currentStatus: boolean): Promise<void> {
    const success = await this.roleListUseCase.toggleRoleStatus(id, currentStatus);
    if (success) {
      this.roleListUseCase.reloadRoles();
      this.triggerSuccessToast();
    }
  }

  private triggerSuccessToast(): void {
    this.showSuccessToast.set(true);
    setTimeout(() => this.showSuccessToast.set(false), 3000);
  }
}
