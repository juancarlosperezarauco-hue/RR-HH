// features/platform/application/platform-subscription-assign.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService, AssignSubscriptionRequest, PlatformSubscription } from '../../../entities/platform/api/platform.service';

export interface PlatformSubscriptionAssignState {
  status: 'idle' | 'loading' | 'success' | 'error';
  subscription: PlatformSubscription | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformSubscriptionAssignUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformSubscriptionAssignState>({
    status: 'idle',
    subscription: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly subscription = computed(() => this.state().subscription);
  readonly error = computed(() => this.state().error);

  async assignSubscription(request: AssignSubscriptionRequest): Promise<boolean> {
    this.state.set({ status: 'loading', subscription: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.assignSubscription(request));
      if (response.success) {
        this.state.set({ status: 'success', subscription: response.data, error: null });
        return true;
      } else {
        this.state.set({ status: 'error', subscription: null, error: response.message });
        return false;
      }
    } catch (err: any) {
      this.state.set({ status: 'error', subscription: null, error: err.message || 'Error al asignar suscripción' });
      return false;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', subscription: null, error: null });
  }
}