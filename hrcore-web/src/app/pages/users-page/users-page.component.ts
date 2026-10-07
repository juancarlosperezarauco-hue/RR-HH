import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { HasPermissionPipe } from '../../shared/api';
import { ToastService } from '../../shared/ui/toast/toast.service';
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
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              Owner Admin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Usuarios del tenant
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Gestiona los accesos, el estado y los roles del equipo que opera dentro del tenant.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="refresh-ccw" [size]="16"></lucide-icon>
              Recargar
            </button>

            <button
              *ngIf="'users.create' | hasPermission"
              type="button"
              (click)="openCreateModal()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#2E7D32] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#256428] shadow-sm">
              <lucide-icon name="plus" [size]="16"></lucide-icon>
              Nuevo usuario
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Usuarios en página</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Cargados actualmente</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Activos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().active }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Pueden acceder al sistema</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Pendientes</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().pending }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Esperan activación por correo</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Inactivos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().inactive }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Suspendidos o deshabilitados</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Filtrados</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().filtered }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Resultados visibles en pantalla</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm space-y-4">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-lg font-bold text-[#1B5E20]">Lista de usuarios</h2>
            <p class="text-sm text-[#6B7D6C]">Filtra por nombre, correo o estado</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <input
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event); applyFilters()"
              type="text"
              placeholder="Buscar usuario"
              class="h-11 w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32] focus:bg-white" />

            <select
              [ngModel]="statusFilter()"
              (ngModelChange)="statusFilter.set($event); applyFilters()"
              class="h-11 w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white">
              <option value="all">Todos los estados</option>
              <option value="ACTIVE">Activos</option>
              <option value="PENDING">Pendientes de activación</option>
              <option value="INACTIVE">Inactivos</option>
            </select>
          </div>
        </div>

        @if (userListUseCase.status() === 'loading' && filteredUsers().length === 0) {
          <div class="flex flex-col items-center justify-center gap-4 p-12 text-[#6B7D6C]">
            <svg class="h-8 w-8 animate-spin text-[#2E7D32]" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p>Cargando usuarios...</p>
          </div>
        }

        @if (userListUseCase.status() === 'error') {
          <div class="rounded-2xl border border-red-200 bg-red-50 p-6 flex items-start gap-4">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mt-0.5 text-red-700"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
            <div>
              <h3 class="font-semibold text-red-700">Error al cargar usuarios</h3>
              <p class="mt-1 text-sm text-red-700/80">{{ userListUseCase.error() }}</p>
              <button (click)="reload()" class="mt-3 cursor-pointer text-sm font-semibold text-red-700 hover:underline">Intentar nuevamente</button>
            </div>
          </div>
        }

        @if (userListUseCase.status() === 'success' || (userListUseCase.status() === 'loading' && filteredUsers().length > 0)) {
          <div class="overflow-x-auto rounded-2xl border border-[#E8F2E2]">
            <table class="min-w-[860px] w-full divide-y divide-[#E8F2E2]">
              <thead class="bg-[#F7FBF3]">
                <tr class="text-left text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">
                  <th class="px-4 py-3">Usuario</th>
                  <th class="px-4 py-3">Correo</th>
                  <th class="px-4 py-3">Estado</th>
                  <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[#EEF5EA] bg-white">
                @for (user of filteredUsers(); track user.id) {
                  <tr class="hover:bg-[#F7FBF3] transition-colors">
                    <td class="px-4 py-3">
                      <p class="font-semibold text-[#1B5E20]">{{ user.firstName }} {{ user.lastName }}</p>
                      <p class="text-xs text-[#6B7D6C]">ID {{ user.id }}</p>
                    </td>
                    <td class="px-4 py-3 text-sm text-[#4F5D4F]">{{ user.email }}</td>
                    <td class="px-4 py-3">
                      <span
                        class="inline-flex rounded-full px-3 py-1 text-[11px] font-bold uppercase tracking-[0.14em]"
                        [class.bg-[#E8F5E9]]="userStatus(user) === 'ACTIVE'"
                        [class.text-[#2E7D32]]="userStatus(user) === 'ACTIVE'"
                        [class.bg-amber-100]="userStatus(user) === 'PENDING'"
                        [class.text-amber-800]="userStatus(user) === 'PENDING'"
                        [class.bg-red-100]="userStatus(user) === 'INACTIVE'"
                        [class.text-red-700]="userStatus(user) === 'INACTIVE'">
                        {{ userStatusLabel(user) }}
                      </span>
                    </td>
                    <td class="px-4 py-3">
                      <div class="flex items-center justify-end gap-2">
                        <button *ngIf="'users.detail' | hasPermission" (click)="openRoleModal(user)" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] hover:bg-[#F1F8E9] transition-colors" title="Gestionar roles">
                          <lucide-icon name="users" [size]="16"></lucide-icon>
                        </button>
                        <button *ngIf="'users.update' | hasPermission" (click)="openEditModal(user)" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] hover:bg-[#F1F8E9] transition-colors" title="Editar usuario">
                          <lucide-icon name="pencil" [size]="16"></lucide-icon>
                        </button>
                        <button
                          *ngIf="(user.active && ('users.deactivate' | hasPermission)) || (!user.active && ('users.activate' | hasPermission))"
                          (click)="handleToggleStatus(user)"
                          class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] hover:bg-[#F1F8E9] transition-colors"
                          [title]="user.active ? 'Desactivar' : 'Activar'">
                          <lucide-icon [name]="user.active ? 'ban' : 'play'" [size]="16"></lucide-icon>
                        </button>
                      </div>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="px-4 py-12 text-center text-[#6B7D6C]">
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
        <div class="app-modal-panel app-modal-panel-sm">
          <div class="app-modal-header border-b border-[#E8F2E2] pb-5">
            <div>
              <h2 class="app-modal-title">Nuevo usuario</h2>
              <p class="app-modal-subtitle">Crea un acceso para un integrante del tenant.</p>
            </div>
            <button (click)="closeCreateModal()" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="x" [size]="20"></lucide-icon>
            </button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto pt-6">
            <app-user-create-form
              [status]="userCreateUseCase.status()"
              (formSubmit)="handleCreateUser($event)"
              (cancel)="closeCreateModal()">
            </app-user-create-form>
          </div>
        </div>
      </div>
    }

    @if (isEditModalOpen() && selectedUserToEdit()) {
      <div class="app-modal-overlay">
        <div class="app-modal-panel app-modal-panel-sm">
          <div class="app-modal-header border-b border-[#E8F2E2] pb-5">
            <div>
              <h2 class="app-modal-title">Editar usuario</h2>
              <p class="app-modal-subtitle">Actualiza nombre, apellido o correo.</p>
            </div>
            <button (click)="closeEditModal()" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="x" [size]="20"></lucide-icon>
            </button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto pt-6">
            <app-user-edit-form
              [status]="userUpdateUseCase.status()"
              [user]="selectedUserToEdit()!"
              (formSubmit)="handleUpdateUser($event)"
              (cancel)="closeEditModal()">
            </app-user-edit-form>
          </div>
        </div>
      </div>
    }

    @if (isRoleModalOpen()) {
      <div class="app-modal-overlay">
        <div class="app-modal-panel w-full max-w-2xl">
          <div class="app-modal-header border-b border-[#E8F2E2] pb-5">
            <div>
              <h2 class="app-modal-title">Asignar roles</h2>
              <p class="app-modal-subtitle">Define qué puede hacer este usuario dentro del tenant.</p>
            </div>
            <button (click)="closeRoleModal()" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="x" [size]="20"></lucide-icon>
            </button>
          </div>

          <div class="max-h-[70vh] overflow-y-auto pt-6">
            <app-user-role-assignment
              [status]="userRoleUseCase.status()"
              [user]="selectedUser()"
              [availableRoles]="userRoleUseCase.availableRoles()"
              [assignedRoles]="userRoleUseCase.userRoles()"
              (submitRoles)="handleAssignRoles($event)"
              (cancel)="closeRoleModal()">
            </app-user-role-assignment>
          </div>
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
        `${user.firstName} ${user.lastName}`.toLowerCase().includes(query) ||
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
  private readonly toastService = inject(ToastService);

  constructor() {
    effect(() => {
      if (this.userCreateUseCase.status() === 'success') {
        this.closeCreateModal();
        this.userCreateUseCase.resetState();
        this.reload();
        this.toastService.success('Usuario creado. Se envió un correo de activación.');
      }
    }, { allowSignalWrites: true });

    effect(() => {
      if (this.userUpdateUseCase.status() === 'success') {
        this.closeEditModal();
        this.userUpdateUseCase.resetState();
        this.reload();
        this.toastService.success('Usuario actualizado correctamente');
      }
    }, { allowSignalWrites: true });

    effect(() => {
      if (this.userRoleUseCase.status() === 'success') {
        this.closeRoleModal();
        this.userRoleUseCase.resetState();
        this.toastService.success('Roles actualizados correctamente');
      }
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.reload();
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
  }

  closeCreateModal(): void {
    if (this.userCreateUseCase.status() === 'loading') return;
    this.isCreateModalOpen.set(false);
  }

  handleCreateUser(request: CreateTenantUserRequest): void {
    this.userCreateUseCase.createUser(request);
  }

  openEditModal(user: TenantUserResponse): void {
    this.userUpdateUseCase.resetState();
    this.selectedUserToEdit.set(user);
    this.isEditModalOpen.set(true);
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

  async handleToggleStatus(user: TenantUserResponse): Promise<void> {
    const confirmMessage = user.active
      ? `¿Estás seguro de desactivar a ${user.firstName} ${user.lastName}?`
      : `¿Estás seguro de activar a ${user.firstName} ${user.lastName}?`;

    if (!confirm(confirmMessage)) {
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
    if (!user) {
      return;
    }

    await this.userRoleUseCase.assignRoles(String(user.id), roleIds);
  }
}
