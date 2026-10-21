import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';
import { HasPermissionPipe } from '../../shared/api';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { ConfirmModalService } from '../../shared/ui/confirm-modal';
import {
  UserCreateFormComponent,
  UserCreateUseCase,
  UserEditFormComponent,
  UserListUseCase,
  UserStatusUseCase,
  UserUpdateUseCase
} from '../../features/user-management';
import { UserRoleAssignmentComponent, UserRoleUseCase } from '../../features/user-role-management';
import { TenantUserResponse, CreateTenantUserRequest, UpdateTenantUserRequest } from '../../entities/user';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { EmployeesService } from '../../entities/employees/api/employees.service';
import { EmployeeSummary } from '../../entities/employees/model/employees.model';

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PlatformPaginationComponent,
    LucideAngularModule,
    HasPermissionPipe,
    UserCreateFormComponent,
    UserEditFormComponent,
    UserRoleAssignmentComponent
  ],
  providers: [CurrencyPipe],
  template: `
    <div class="space-y-6 relative">
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Gestión de Organización
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Usuarios del tenant
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Administra el ciclo de vida de los usuarios de tu organización, vinculaciones con empleados, roles y control de acceso.
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
              *ngIf="'users.create' | hasPermission"
              type="button"
              (click)="openCreateModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 shadow-sm">
              <lucide-icon name="plus" [size]="16"></lucide-icon>
              Nuevo usuario
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Usuarios en página</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Cargados actualmente</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Activos</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().active }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Pueden acceder al sistema</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Pendientes</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().pending }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Esperan activación por correo</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Inactivos</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().inactive }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Suspendidos o deshabilitados</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm space-y-4">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-lg font-bold text-accent-foreground">Lista de usuarios</h2>
            <p class="text-sm text-muted-foreground">Filtra por nombre, correo, empleado vinculado o estado</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <input
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event); applyFilters()"
              type="text"
              placeholder="Buscar usuario o empleado..."
              class="h-11 w-full rounded-2xl border border-border bg-muted px-4 text-sm text-accent-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary focus:bg-card" />

            <select
              [ngModel]="statusFilter()"
              (ngModelChange)="statusFilter.set($event); applyFilters()"
              class="h-11 w-full rounded-2xl border border-border bg-muted px-4 text-sm text-accent-foreground outline-none transition-colors focus:border-primary focus:bg-card">
              <option value="all">Todos los estados</option>
              <option value="ACTIVE">Activos</option>
              <option value="PENDING">Pendientes de activación</option>
              <option value="INACTIVE">Inactivos</option>
            </select>
          </div>
        </div>

        @if (userListUseCase.status() === 'loading' && filteredUsers().length === 0) {
          <div class="flex flex-col items-center justify-center gap-4 p-12 text-muted-foreground">
            <svg class="h-8 w-8 animate-spin text-primary" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p>Cargando usuarios...</p>
          </div>
        }

        @if (userListUseCase.status() === 'error') {
          <div class="rounded-2xl border border-destructive/20 bg-destructive/5 p-6 flex items-start gap-4">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mt-0.5 text-destructive"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
            <div>
              <h3 class="font-semibold text-destructive">Error al cargar usuarios</h3>
              <p class="mt-1 text-sm text-destructive/80">{{ userListUseCase.error() }}</p>
              <button (click)="reload()" class="mt-3 cursor-pointer text-sm font-semibold text-destructive hover:underline">Intentar nuevamente</button>
            </div>
          </div>
        }

        @if (userListUseCase.status() === 'success' || (userListUseCase.status() === 'loading' && filteredUsers().length > 0)) {
          <div class="overflow-x-auto rounded-2xl border border-border">
            <table class="min-w-[860px] w-full divide-y divide-border">
              <thead class="bg-background">
                <tr class="text-left text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">
                  <th class="px-4 py-3">Usuario</th>
                  <th class="px-4 py-3">Correo</th>
                  <th class="px-4 py-3">Empleado vinculado</th>
                  <th class="px-4 py-3">Estado</th>
                  <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-border bg-card">
                @for (user of filteredUsers(); track user.id) {
                  <tr class="hover:bg-background transition-colors">
                    <td class="px-4 py-3">
                      <p class="font-semibold text-accent-foreground">
                        {{ user.displayName || (user.firstName + ' ' + user.lastName) }}
                      </p>
                      <p class="text-xs text-muted-foreground">ID: {{ user.id }}</p>
                    </td>
                    <td class="px-4 py-3 text-sm text-muted-foreground">{{ user.email }}</td>
                    <td class="px-4 py-3 text-sm">
                      @if (user.employeeNumber) {
                        <span class="inline-flex items-center gap-1.5 rounded-full border border-border bg-muted/60 px-2.5 py-1 text-xs font-medium text-accent-foreground">
                          <lucide-icon name="user-check" [size]="12" class="text-primary"></lucide-icon>
                          {{ user.employeeNumber }} — {{ user.employeeFullName }}
                        </span>
                      } @else {
                        <span class="text-xs text-muted-foreground/60 italic">Sin vincular</span>
                      }
                    </td>
                    <td class="px-4 py-3">
                      <span
                        class="inline-flex rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-[0.14em]"
                        [class.bg-accent]="userStatus(user) === 'ACTIVE'"
                        [class.text-primary]="userStatus(user) === 'ACTIVE'"
                        [class.bg-warning/10]="userStatus(user) === 'PENDING'"
                        [class.text-warning]="userStatus(user) === 'PENDING'"
                        [class.bg-destructive/10]="userStatus(user) === 'INACTIVE'"
                        [class.text-destructive]="userStatus(user) === 'INACTIVE'">
                        {{ userStatusLabel(user) }}
                      </span>
                    </td>
                    <td class="px-4 py-3">
                      <div class="flex items-center justify-end gap-2">
                        <button *ngIf="canReadUserRoles(user)" (click)="openRoleModal(user)" class="cursor-pointer rounded-full border border-border p-2 text-primary hover:bg-accent transition-colors" title="Gestionar roles">
                          <lucide-icon name="users" [size]="16"></lucide-icon>
                        </button>
                        <button *ngIf="('users.update' | hasPermission) && !isCurrentOwner(user)" (click)="openEditModal(user)" class="cursor-pointer rounded-full border border-border p-2 text-primary hover:bg-accent transition-colors" title="Editar usuario">
                          <lucide-icon name="pencil" [size]="16"></lucide-icon>
                        </button>
                        <button
                          *ngIf="canToggleStatus(user)"
                          (click)="handleToggleStatus(user)"
                          class="cursor-pointer rounded-full border border-border p-2 text-primary hover:bg-accent transition-colors"
                          [title]="user.active ? 'Desactivar acceso' : 'Activar acceso'">
                          <lucide-icon [name]="user.active ? 'ban' : 'play'" [size]="16"></lucide-icon>
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="px-4 py-12 text-center text-muted-foreground">
                      No hay usuarios que coincidan con los filtros actuales.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <app-platform-pagination
            class="block"
            [currentPage]="currentPage()"
            [totalPages]="totalPages()"
            [totalElements]="totalElements()"
            [isLoading]="userListUseCase.status() === 'loading'"
            (pageChange)="onPageChange($event)">
          </app-platform-pagination>
        }
      </section>
    </div>

    @if (isCreateModalOpen()) {
      <div class="app-modal-overlay">
        <div class="app-modal-panel max-w-3xl">
          <div class="app-modal-header">
            <div>
              <div class="mb-1 inline-flex items-center gap-1.5 rounded-full border border-border bg-muted/50 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest text-primary">
                <lucide-icon name="user-plus" [size]="12"></lucide-icon>
                Control de acceso
              </div>
              <h2 class="app-modal-title">Nuevo usuario</h2>
              <p class="app-modal-subtitle">Crea un acceso para un integrante del tenant y asócialo opcionalmente a un empleado.</p>
            </div>
            <button type="button" (click)="closeCreateModal()" class="cursor-pointer rounded-full border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground" aria-label="Cerrar">
              <lucide-icon name="x" [size]="18"></lucide-icon>
            </button>
          </div>
          <app-user-create-form
            class="flex min-h-0 flex-1 flex-col overflow-hidden"
            [status]="userCreateUseCase.status()"
            [error]="userCreateUseCase.error()"
            [canAssignRoles]="permissionService.isCurrentOwner()"
            [availableRoles]="userRoleUseCase.availableRoles()"
            [availableEmployees]="availableEmployees()"
            (formSubmit)="handleCreateUser($event)"
            (cancel)="closeCreateModal()">
          </app-user-create-form>
        </div>
      </div>
    }

    @if (isEditModalOpen() && selectedUserToEdit()) {
      <div class="app-modal-overlay">
        <div class="app-modal-panel max-w-2xl">
          <div class="app-modal-header">
            <div>
              <div class="mb-1 inline-flex items-center gap-1.5 rounded-full border border-border bg-muted/50 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest text-primary">
                <lucide-icon name="pencil" [size]="12"></lucide-icon>
                Actualización de cuenta
              </div>
              <h2 class="app-modal-title">Editar usuario</h2>
              <p class="app-modal-subtitle">Actualiza nombre, apellido, correo o empleado vinculado.</p>
            </div>
            <button type="button" (click)="closeEditModal()" class="cursor-pointer rounded-full border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground" aria-label="Cerrar">
              <lucide-icon name="x" [size]="18"></lucide-icon>
            </button>
          </div>
          <app-user-edit-form
            class="flex min-h-0 flex-1 flex-col overflow-hidden"
            [status]="userUpdateUseCase.status()"
            [user]="selectedUserToEdit()!"
            [availableEmployees]="availableEmployees()"
            (formSubmit)="handleUpdateUser($event)"
            (cancel)="closeEditModal()">
          </app-user-edit-form>
        </div>
      </div>
    }

    @if (isRoleModalOpen()) {
      <div class="app-modal-overlay">
        <div class="app-modal-panel max-w-2xl">
          <div class="app-modal-header">
            <div>
              <div class="mb-1 inline-flex items-center gap-1.5 rounded-full border border-border bg-muted/50 px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-widest text-primary">
                <lucide-icon name="shield-check" [size]="12"></lucide-icon>
                Permisos RBAC
              </div>
              <h2 class="app-modal-title">Asignar roles</h2>
              <p class="app-modal-subtitle">Define qué puede hacer este usuario dentro del tenant.</p>
            </div>
            <button type="button" (click)="closeRoleModal()" class="cursor-pointer rounded-full border border-border p-2 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground" aria-label="Cerrar">
              <lucide-icon name="x" [size]="18"></lucide-icon>
            </button>
          </div>

          <app-user-role-assignment
            class="flex min-h-0 flex-1 flex-col overflow-hidden"
            [status]="userRoleUseCase.status()"
            [user]="selectedUser()"
            [availableRoles]="userRoleUseCase.availableRoles()"
            [assignedRoles]="userRoleUseCase.userRoles()"
            (submitRoles)="handleAssignRoles($event)"
            (cancel)="closeRoleModal()">
          </app-user-role-assignment>
        </div>
      </div>
    }
  `
})
export class UsersPageComponent implements OnInit {
  public readonly userListUseCase = inject(UserListUseCase);
  public readonly userCreateUseCase = inject(UserCreateUseCase);
  public readonly userUpdateUseCase = inject(UserUpdateUseCase);
  public readonly userStatusUseCase = inject(UserStatusUseCase);
  public readonly userRoleUseCase = inject(UserRoleUseCase);
  readonly permissionService = inject(PermissionService);
  private readonly authFacade = inject(AuthFacade);
  private readonly employeesService = inject(EmployeesService);
  private readonly toastService = inject(ToastService);
  private readonly confirmModalService = inject(ConfirmModalService);

  readonly availableEmployees = signal<EmployeeSummary[]>([]);
  readonly isCreateModalOpen = signal(false);
  readonly isEditModalOpen = signal(false);
  readonly isRoleModalOpen = signal(false);
  readonly selectedUser = signal<TenantUserResponse | null>(null);
  readonly selectedUserToEdit = signal<TenantUserResponse | null>(null);
  readonly pageSize = 20;
  readonly searchQuery = signal('');
  readonly statusFilter = signal<'all' | 'ACTIVE' | 'PENDING' | 'INACTIVE'>('all');

  readonly currentPage = computed(() => this.userListUseCase.page()?.number ?? 0);
  readonly totalPages = computed(() => this.userListUseCase.page()?.totalPages ?? 0);
  readonly totalElements = computed(() => this.userListUseCase.page()?.totalElements ?? 0);
  readonly filteredUsers = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const status = this.statusFilter();

    return this.userListUseCase.data().filter((user) => {
      const matchesQuery =
        !query ||
        `${user.firstName || ''} ${user.lastName || ''}`.toLowerCase().includes(query) ||
        (user.displayName && user.displayName.toLowerCase().includes(query)) ||
        (user.employeeNumber && user.employeeNumber.toLowerCase().includes(query)) ||
        (user.employeeFullName && user.employeeFullName.toLowerCase().includes(query)) ||
        user.email.toLowerCase().includes(query) ||
        String(user.id).includes(query);

      const matchesStatus = status === 'all' ? true : this.userStatus(user) === status;
      return matchesQuery && matchesStatus;
    });
  });

  readonly stats = computed(() => {
    const users = this.userListUseCase.data();
    const active = users.filter((user) => this.userStatus(user) === 'ACTIVE').length;
    const pending = users.filter((user) => this.userStatus(user) === 'PENDING').length;
    const inactive = users.filter((user) => this.userStatus(user) === 'INACTIVE').length;
    const filtered = this.filteredUsers().length;
    return { total: users.length, active, pending, inactive, filtered };
  });

  constructor() {
    effect(() => {
      if (this.userCreateUseCase.status() === 'success') {
        this.closeCreateModal();
        this.userCreateUseCase.resetState();
        this.reload();
        this.toastService.success('Usuario registrado exitosamente');
      } else if (this.userCreateUseCase.status() === 'error') {
        this.toastService.error(this.userCreateUseCase.error() || 'Error al crear usuario');
      }
    });

    effect(() => {
      if (this.userUpdateUseCase.status() === 'success') {
        this.closeEditModal();
        this.userUpdateUseCase.resetState();
        this.reload();
        this.toastService.success('Usuario actualizado correctamente');
      }
    });

    effect(() => {
      if (this.userRoleUseCase.status() === 'success') {
        this.closeRoleModal();
        this.userRoleUseCase.resetState();
        this.toastService.success('Roles actualizados correctamente');
      }
    });
  }

  ngOnInit(): void {
    void this.loadEmployees();
    void this.reload();
  }

  async loadEmployees(): Promise<void> {
    try {
      const response = await firstValueFrom(this.employeesService.list(0, 100, { status: 'ACTIVE' }));
      if (response?.success && response.data) {
        this.availableEmployees.set(response.data.content);
      }
    } catch {
      // Ignora si falla la precarga de empleados opcionales
    }
  }

  async reload(): Promise<void> {
    await this.userListUseCase.loadUsers(this.currentPage(), this.pageSize);
  }

  onPageChange(page: number): void {
    this.userListUseCase.loadUsers(page, this.pageSize);
  }

  applyFilters(): void {}

  userStatus(user: TenantUserResponse): 'ACTIVE' | 'PENDING' | 'INACTIVE' {
    const raw = (user.status || '').toString().trim().toUpperCase();
    if (raw === 'ACTIVE' || raw === 'PENDING' || raw === 'INACTIVE') {
      return raw;
    }

    return user.active ? 'ACTIVE' : 'INACTIVE';
  }

  userStatusLabel(user: TenantUserResponse): string {
    const status = this.userStatus(user);
    if (status === 'PENDING') {
      return 'Pendiente de activación';
    }
    if (status === 'ACTIVE') {
      return 'Activo';
    }
    return 'Inactivo';
  }

  openCreateModal(): void {
    this.userCreateUseCase.resetState();
    this.isCreateModalOpen.set(true);
    if (this.permissionService.isCurrentOwner()) void this.userRoleUseCase.loadAvailableRoles();
    void this.loadEmployees();
  }

  closeCreateModal(): void {
    if (this.userCreateUseCase.status() === 'loading') return;
    this.isCreateModalOpen.set(false);
  }

  handleCreateUser(request: CreateTenantUserRequest): void {
    this.userCreateUseCase.createUser({
      ...request,
      roleIds: this.permissionService.isCurrentOwner() ? [...new Set(request.roleIds ?? [])] : []
    });
  }

  openEditModal(user: TenantUserResponse): void {
    this.userUpdateUseCase.resetState();
    this.selectedUserToEdit.set(user);
    this.isEditModalOpen.set(true);
    void this.loadEmployees();
  }

  closeEditModal(): void {
    if (this.userUpdateUseCase.status() === 'loading') return;
    this.isEditModalOpen.set(false);
    setTimeout(() => this.selectedUserToEdit.set(null), 300);
  }

  handleUpdateUser(request: UpdateTenantUserRequest): void {
    const user = this.selectedUserToEdit();
    if (user) {
      this.userUpdateUseCase.updateUser(String(user.id), request);
    }
  }

  canToggleStatus(user: TenantUserResponse): boolean {
    if (this.isCurrentOwner(user)) return false;
    if (this.isCurrentUser(user)) return false;
    return (user.active && this.permissionService.hasPermission('users.deactivate')) ||
           (!user.active && this.permissionService.hasPermission('users.activate'));
  }

  async handleToggleStatus(user: TenantUserResponse): Promise<void> {
    if (this.isCurrentOwner(user)) {
      this.toastService.warning('No es posible desactivar al propietario de la organización');
      return;
    }
    if (this.isCurrentUser(user)) {
      this.toastService.warning('No puedes desactivar tu propia cuenta activa de administrador');
      return;
    }

    const confirmed = await this.confirmModalService.confirm({
      title: user.active ? '¿Desactivar usuario?' : '¿Activar usuario?',
      message: user.active
        ? `¿Estás seguro de desactivar a ${user.firstName} ${user.lastName}?`
        : `¿Estás seguro de activar a ${user.firstName} ${user.lastName}?`,
      details: user.active
        ? 'El usuario perderá de inmediato el acceso al sistema y no podrá iniciar sesión en la organización.'
        : 'El usuario recuperará el acceso al sistema conforme a los roles y permisos que tenga asignados.',
      confirmText: user.active ? 'Desactivar Usuario' : 'Activar Usuario',
      cancelText: 'Cancelar',
      variant: user.active ? 'danger' : 'success'
    });

    if (!confirmed) {
      return;
    }

    const success = await this.userStatusUseCase.toggleStatus(String(user.id), user.active);
    if (success) {
      this.toastService.success(user.active ? 'Usuario desactivado correctamente' : 'Usuario activado correctamente');
      await this.reload();
    } else {
      this.toastService.error(this.userStatusUseCase.error() || 'No se pudo cambiar el estado del usuario');
    }
  }

  async openRoleModal(user: TenantUserResponse): Promise<void> {
    if (!this.canReadUserRoles(user)) return;
    this.selectedUser.set(user);
    this.isRoleModalOpen.set(true);
    await this.userRoleUseCase.loadRolesForUser(String(user.id));
  }

  closeRoleModal(): void {
    if (this.userRoleUseCase.status() === 'loading') return;
    this.isRoleModalOpen.set(false);
    this.selectedUser.set(null);
    this.userRoleUseCase.resetState();
  }

  async handleAssignRoles(roleIds: string[]): Promise<void> {
    const user = this.selectedUser();
    if (!user || !this.canAssignUserRoles()) {
      return;
    }

    await this.userRoleUseCase.assignRoles(String(user.id), roleIds);
    if (this.userRoleUseCase.status() === 'success') {
      this.toastService.success('Roles asignados exitosamente');
      this.closeRoleModal();
      await this.reload();
    } else if (this.userRoleUseCase.status() === 'error') {
      this.toastService.error(this.userRoleUseCase.error() || 'Error al asignar roles');
    }
  }

  canReadUserRoles(user: TenantUserResponse): boolean {
    return this.permissionService.isCurrentOwner() && this.permissionService.hasPermission('access.users.roles.read') && !this.isCurrentOwner(user);
  }

  canAssignUserRoles(): boolean {
    return this.permissionService.isCurrentOwner() && this.permissionService.hasPermission('access.users.roles.assign');
  }

  isCurrentOwner(user: TenantUserResponse): boolean {
    const current = this.authFacade.currentUser();
    return !!current?.owner && current.id === user.id;
  }

  isCurrentUser(user: TenantUserResponse): boolean {
    const current = this.authFacade.currentUser();
    return current?.id === user.id;
  }
}
