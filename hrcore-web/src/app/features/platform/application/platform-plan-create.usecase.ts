// features/platform/application/platform-plan-create.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService, CreatePlanRequest, PlatformPlan } from '../../../entities/platform/api/platform.service';

export interface PlatformPlanCreateState {
  status: 'idle' | 'loading' | 'success' | 'error';
  createdPlan: PlatformPlan | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformPlanCreateUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformPlanCreateState>({
    status: 'idle',
    createdPlan: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly createdPlan = computed(() => this.state().createdPlan);
  readonly error = computed(() => this.state().error);

  async createPlan(request: CreatePlanRequest): Promise<boolean> {
    this.state.set({ status: 'loading', createdPlan: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.createPlan(request));
      if (response.success) {
        this.state.set({ status: 'success', createdPlan: response.data, error: null });
        return true;
      } else {
        this.state.set({ status: 'error', createdPlan: null, error: response.message });
        return false;
      }
    } catch (err: any) {
      this.state.set({ status: 'error', createdPlan: null, error: err.message || 'Error al crear plan' });
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', createdPlan: null, error: null });
  }
}