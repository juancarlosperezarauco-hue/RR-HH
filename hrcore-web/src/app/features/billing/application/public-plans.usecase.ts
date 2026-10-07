import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { PublicBillingPlan, PublicBillingService } from '../../../entities/billing';

export interface PublicPlansState {
  status: 'idle' | 'loading' | 'success' | 'error';
  plans: PublicBillingPlan[];
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class PublicPlansUseCase {
  private readonly service = inject(PublicBillingService);

  private readonly state = signal<PublicPlansState>({
    status: 'idle',
    plans: [],
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly plans = computed(() => this.state().plans);
  readonly error = computed(() => this.state().error);

  async loadPlans(): Promise<void> {
    this.state.set({ status: 'loading', plans: this.state().plans, error: null });

    try {
      const response = await firstValueFrom(this.service.listPublicPlans().pipe(timeout(15000)));

      if (response.success && response.data) {
        this.state.set({
          status: 'success',
          plans: [...response.data]
            .filter((plan) => plan.publicVisible && plan.active)
            .sort((a, b) => a.sortOrder - b.sortOrder),
          error: null
        });
        return;
      }

      this.state.set({
        status: 'error',
        plans: [],
        error: response.message || 'No se pudieron cargar los planes'
      });
    } catch (error: any) {
      this.state.set({
        status: 'error',
        plans: [],
        error: error.error?.message || error.message || 'Error al conectar con el servidor'
      });
    }
  }
}
