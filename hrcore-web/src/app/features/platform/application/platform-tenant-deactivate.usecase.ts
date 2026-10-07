import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService } from '../../../entities/platform/api/platform.service';

export interface PlatformTenantDeactivateState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformTenantDeactivateUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformTenantDeactivateState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async deactivateTenant(id: string): Promise<boolean> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.platformService.deactivateTenant(id));
      if (response.success) {
        this.state.set({ status: 'success', error: null });
        return true;
      } else {
        this.state.set({ status: 'error', error: response.message });
        return false;
      }
    } catch (err: any) {
      this.state.set({ status: 'error', error: err.message || 'Error al desactivar tenant' });
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}