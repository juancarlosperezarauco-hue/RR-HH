// features/platform/application/platform-plan-activate.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService } from '../../../entities/platform/api/platform.service';

export interface PlatformPlanActivateState {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformPlanActivateUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformPlanActivateState>({
    status: 'idle',
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly error = computed(() => this.state().error);

  async activatePlan(id: string): Promise<boolean> {
    this.state.set({ status: 'loading', error: null });

    try {
      const response = await firstValueFrom(this.platformService.activatePlan(id));
      if (response.success) {
        this.state.set({ status: 'success', error: null });
        return true;
      } else {
        this.state.set({ status: 'error', error: response.message });
        return false;
      }
    } catch (err: any) {
      this.state.set({ status: 'error', error: err.message || 'Error al activar plan' });
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', error: null });
  }
}