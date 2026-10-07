// features/platform/application/platform-plan-deactivate.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService } from '../../../entities/platform/api/platform.service';

export interface PlatformPlanDeactivateState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformPlanDeactivateUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformPlanDeactivateState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async deactivatePlan(id: string): Promise<boolean> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.platformService.deactivatePlan(id));
      if (response.success) {
        this.state.set({ status: 'success', error: null });
        return true;
      } else {
        this.state.set({ status: 'error', error: response.message });
        return false;
      }
    } catch (err: any) {
      this.state.set({ status: 'error', error: err.message || 'Error al desactivar plan' });
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}