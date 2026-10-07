import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import {
  PendingCheckout,
  PendingCheckoutStorageService,
  PublicBillingService,
  PublicCheckoutActivationStatusResponse
} from '../../../entities/billing';

export interface CheckoutActivationState {
  status: 'idle' | 'loading' | 'active' | 'pending' | 'failed' | 'error';
  data: PublicCheckoutActivationStatusResponse | null;
  pendingCheckout: PendingCheckout | null;
  error: string | null;
  attempts: number;
}

@Injectable({
  providedIn: 'root'
})
export class CheckoutActivationStatusUseCase {
  private readonly billingService = inject(PublicBillingService);
  private readonly storage = inject(PendingCheckoutStorageService);

  private readonly state = signal<CheckoutActivationState>({
    status: 'idle',
    data: null,
    pendingCheckout: null,
    error: null,
    attempts: 0
  });

  readonly status = computed(() => this.state().status);
  readonly data = computed(() => this.state().data);
  readonly pendingCheckout = computed(() => this.state().pendingCheckout);
  readonly error = computed(() => this.state().error);
  readonly attempts = computed(() => this.state().attempts);

  loadPendingCheckout(): PendingCheckout | null {
    const pending = this.storage.get();
    this.state.update((current) => ({
      ...current,
      pendingCheckout: pending
    }));
    return pending;
  }

  async checkStatus(stripeSessionId: string): Promise<void> {
    const pending = this.storage.get();

    this.state.update((current) => ({
      ...current,
      status: current.status === 'idle' ? 'loading' : current.status,
      pendingCheckout: pending,
      error: null,
      attempts: current.attempts + 1
    }));

    try {
      const response = await firstValueFrom(
        this.billingService.getCheckoutActivationStatus(stripeSessionId).pipe(timeout(15000))
      );

      if (!response.success || !response.data) {
        this.state.update((current) => ({
          ...current,
          status: 'error',
          data: null,
          error: response.message || 'No se pudo verificar la activación del plan'
        }));
        return;
      }

      const data = response.data;

      if (data.active || data.activationStatus === 'ACTIVE') {
        this.clearPendingCheckout();
        this.state.update((current) => ({
          ...current,
          status: 'active',
          data,
          error: null
        }));
        return;
      }

      if (data.failed || data.activationStatus === 'FAILED') {
        this.state.update((current) => ({
          ...current,
          status: 'failed',
          data,
          error: null
        }));
        return;
      }

      this.state.update((current) => ({
        ...current,
        status: 'pending',
        data,
        error: null
      }));
    } catch (err: any) {
      this.state.update((current) => ({
        ...current,
        status: 'error',
        error: err.error?.message || err.message || 'Error al consultar el estado del pago'
      }));
    }
  }

  clearPendingCheckout(): void {
    this.storage.clear();
    this.state.update((current) => ({
      ...current,
      pendingCheckout: null
    }));
  }

  reset(): void {
    this.state.set({
      status: 'idle',
      data: null,
      pendingCheckout: null,
      error: null,
      attempts: 0
    });
  }
}
