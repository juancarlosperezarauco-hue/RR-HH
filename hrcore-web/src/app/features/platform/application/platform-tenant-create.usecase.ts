import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService, CreateTenantRequest } from '../../../entities/platform/api/platform.service';

export interface PlatformTenantCreateState {
  status: 'idle' | 'loading' | 'success' | 'error';
  createdTenant: any | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformTenantCreateUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformTenantCreateState>({
    status: 'idle',
    createdTenant: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly createdTenant = computed(() => this.state().createdTenant);
  readonly error = computed(() => this.state().error);

  async createTenant(request: CreateTenantRequest): Promise<boolean> {
    this.state.set({ status: 'loading', createdTenant: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.createTenant(request));
      if (response.success) {
        this.state.set({ status: 'success', createdTenant: response.data, error: null });
        return true;
      } else {
        this.state.set({ status: 'error', createdTenant: null, error: response.message });
        return false;
      }
    } catch (err: any) {
      this.state.set({ status: 'error', createdTenant: null, error: err.message || 'Error al crear tenant' });
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', createdTenant: null, error: null });
  }
}