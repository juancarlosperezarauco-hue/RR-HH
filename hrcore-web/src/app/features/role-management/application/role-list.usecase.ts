import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AccessService, PageResponse, TenantRoleResponse } from '../../../entities/access';

export interface RoleListState {
  status: 'idle' | 'loading' | 'success' | 'error';
  page: PageResponse<TenantRoleResponse> | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class RoleListUseCase {
  private readonly accessService = inject(AccessService);

  private readonly state = signal<RoleListState>({
    status: 'idle',
    page: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly page = computed(() => this.state().page);
  readonly data = computed(() => this.state().page?.content ?? []);
  readonly error = computed(() => this.state().error);

  async loadRoles(page = 0, size = 500): Promise<void> {
    this.state.set({ status: 'loading', page: this.state().page, error: null });

    try {
      const response = await firstValueFrom(this.accessService.getRoles(page, size));

      if (response.success && response.data) {
        this.state.set({ 
          status: 'success', 
          page: response.data, 
          error: null 
        });
      } else {
        this.state.set({ 
          status: 'error', 
          page: null, 
          error: response.message || 'No se pudieron cargar los roles' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', page: null, error: errorMsg });
    }
  }

  async reloadRoles(): Promise<void> {
    const currentPage = this.state().page;
    this.state.set({ status: 'loading', page: currentPage, error: null });

    try {
      const response = await firstValueFrom(this.accessService.getRoles(currentPage?.number ?? 0, currentPage?.size ?? 500));

      if (response.success && response.data) {
        this.state.set({ 
          status: 'success', 
          page: response.data, 
          error: null 
        });
      } else {
        this.state.set({ 
          status: 'error', 
          page: currentPage,
          error: response.message || 'No se pudieron recargar los roles' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', page: currentPage, error: errorMsg });
    }
  }

  // Métodos extra para activar/desactivar (toggle) en la lista si lo necesitamos
  async toggleRoleStatus(id: string, currentActiveStatus: boolean): Promise<boolean> {
    try {
      if (currentActiveStatus) {
        await firstValueFrom(this.accessService.deactivateRole(id));
      } else {
        await firstValueFrom(this.accessService.activateRole(id));
      }
      return true; // éxito
    } catch (error) {
      console.error('Error toggling role status', error);
      return false; // fallo
    }
  }
}
