// features/platform/application/platform-subscription-get-by-id.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService, PlatformSubscription } from '../../../entities/platform/api/platform.service';

export interface PlatformSubscriptionDetailState {
  status: 'idle' | 'loading' | 'success' | 'error';
  subscription: PlatformSubscription | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformSubscriptionGetByIdUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformSubscriptionDetailState>({
    status: 'idle',
    subscription: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly subscription = computed(() => this.state().subscription);
  readonly error = computed(() => this.state().error);

  async loadSubscription(id: string): Promise<void> {
    this.state.set({ status: 'loading', subscription: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.getSubscriptionById(id));
      if (response.success) {
        this.state.set({ status: 'success', subscription: response.data, error: null });
      } else {
        this.state.set({ status: 'error', subscription: null, error: response.message });
      }
    } catch (err: any) {
      this.state.set({ status: 'error', subscription: null, error: err.message || 'Error al cargar suscripción' });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', subscription: null, error: null });
  }
}