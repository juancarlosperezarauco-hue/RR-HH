import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AccessService, AssignUserRolesRequest, TenantRoleResponse, UserRolesResponse } from '../../../entities/access';

export interface UserRoleState {
  status: 'idle' | 'loading' | 'success' | 'error';
  userId: string | null;
  userRoles: TenantRoleResponse[];
  availableRoles: TenantRoleResponse[];
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class UserRoleUseCase {
  private readonly accessService = inject(AccessService);

  private readonly state = signal<UserRoleState>({
    status: 'idle',
    userId: null,
    userRoles: [],
    availableRoles: [],
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly userRoles = computed(() => this.state().userRoles);
  readonly availableRoles = computed(() => this.state().availableRoles);
  readonly error = computed(() => this.state().error);

  async loadRolesForUser(userId: string): Promise<void> {
    this.state.set({ status: 'loading', userId, userRoles: [], availableRoles: [], error: null });

    try {
      // Cargamos en paralelo los roles del usuario y todos los roles del tenant para mostrar opciones
      const [userRolesRes, allRolesRes] = await Promise.all([
        firstValueFrom(this.accessService.getUserRoles(userId)),
        this.loadAllRoles()
      ]);

      if (userRolesRes.success && userRolesRes.data && userRolesRes.data.userId === userId && allRolesRes.length >= 0) {
        this.state.set({ 
          status: 'idle', // Lo dejamos ready pero idle para asignación
          userId,
          userRoles: userRolesRes.data.roles,
          availableRoles: allRolesRes.filter(r => r.active && !r.systemRole && r.code !== 'OWNER_ADMIN'),
          error: null 
        });
      } else {
        this.state.set({ 
          status: 'error', 
          userId,
          userRoles: [], 
          availableRoles: [],
          error: 'Error al cargar los roles disponibles o asignados.' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', userId, userRoles: [], availableRoles: [], error: errorMsg });
    }
  }

  async assignRoles(userId: string, roleIds: string[]): Promise<void> {
    const currentState = this.state();
    this.state.set({ ...currentState, status: 'loading', error: null });

    try {
      const request: AssignUserRolesRequest = { roleIds: [...new Set(roleIds)] };
      const response = await firstValueFrom(this.accessService.assignUserRoles(userId, request));

      if (response.success && response.data && response.data.userId === userId) {
        this.state.set({ 
          ...currentState,
          status: 'success', 
          userRoles: response.data.roles,
          error: null 
        });
      } else {
        this.state.set({ 
          ...currentState,
          status: 'error', 
          error: response.message || 'Error al asignar roles' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ ...currentState, status: 'error', error: errorMsg });
    }
  }

  async loadAvailableRoles(): Promise<void> {
    try {
      const roles = await this.loadAllRoles();
      this.state.update(state => ({ ...state, availableRoles: roles.filter(role => role.active && !role.systemRole && role.code !== 'OWNER_ADMIN') }));
    } catch {
      this.state.update(state => ({ ...state, availableRoles: [], error: 'No se pudieron cargar los roles disponibles' }));
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', userId: null, userRoles: [], availableRoles: [], error: null });
  }

  private async loadAllRoles(): Promise<TenantRoleResponse[]> {
    const roles: TenantRoleResponse[] = [];
    let page = 0; let last = false;
    while (!last) {
      const response = await firstValueFrom(this.accessService.getRoles(page, 200));
      if (!response.success || !response.data) throw new Error(response.message || 'No se pudieron cargar los roles');
      roles.push(...response.data.content); last = response.data.last || response.data.content.length === 0; page += 1;
    }
    return roles;
  }
}
