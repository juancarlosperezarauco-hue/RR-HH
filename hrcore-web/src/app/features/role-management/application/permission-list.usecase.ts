import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AccessService, PageResponse, SystemPermissionResponse } from '../../../entities/access';

export interface PermissionListState {
  status: 'idle' | 'loading' | 'success' | 'error';
  page: PageResponse<SystemPermissionResponse> | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class PermissionListUseCase {
  private readonly accessService = inject(AccessService);

  private readonly state = signal<PermissionListState>({
    status: 'idle',
    page: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly page = computed(() => this.state().page);
  readonly data = computed(() => this.state().page?.content ?? []);
  readonly error = computed(() => this.state().error);

  async loadPermissions(page = 0, size = 500): Promise<void> {
    // Si ya los tenemos cargados, no volvemos a llamar (cache simple), los permisos rara vez cambian.
    if (this.state().page && this.state().status === 'success') {
      return;
    }

    this.state.set({ status: 'loading', page: this.state().page, error: null });

    try {
      const response = await firstValueFrom(this.accessService.getPermissions(page, size));

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
          error: response.message || 'No se pudieron cargar los permisos del sistema' 
        });
      }
    } catch (err: any) {
      const errorMsg = err.error?.message || err.message || 'Error al conectar con el servidor';
      this.state.set({ status: 'error', page: null, error: errorMsg });
    }
  }
}
