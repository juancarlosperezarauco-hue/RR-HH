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
        firstValueFrom(this.accessService.getRoles(0, 500))
      ]);

      if (userRolesRes.success && allRolesRes.success) {
        this.state.set({ 
          status: 'idle', // Lo dejamos ready pero idle para asignación
          userId,
          userRoles: userRolesRes.data.roles,
          availableRoles: allRolesRes.data.content.filter(r => r.active), // solo asignamos roles activos
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
      const request: AssignUserRolesRequest = { roleIds };
      const response = await firstValueFrom(this.accessService.assignUserRoles(userId, request));

      if (response.success && response.data) {
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

  resetState(): void {
    this.state.set({ status: 'idle', userId: null, userRoles: [], availableRoles: [], error: null });
  }
}
