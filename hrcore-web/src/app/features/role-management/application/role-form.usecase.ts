import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AccessService, CreateTenantRoleRequest, UpdateTenantRoleRequest } from '../../../entities/access';

export interface RoleFormState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class RoleFormUseCase {
  private readonly accessService = inject(AccessService);

  private readonly state = signal<RoleFormState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async createRole(request: CreateTenantRoleRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.accessService.createRole(request));

      if (response.success && response.data) {
        this.state.set({ status: 'success', error: null });
      } else {
        this.state.set({ 
          status: 'error', 
          error: response.message || 'Error al crear el rol' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', error: errorMsg });
    }
  }

  async updateRole(id: string, request: UpdateTenantRoleRequest): Promise<void> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.accessService.updateRole(id, request));

      if (response.success && response.data) {
        this.state.set({ status: 'success', error: null });
      } else {
        this.state.set({ 
          status: 'error', 
          error: response.message || 'Error al actualizar el rol' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', error: errorMsg });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}
