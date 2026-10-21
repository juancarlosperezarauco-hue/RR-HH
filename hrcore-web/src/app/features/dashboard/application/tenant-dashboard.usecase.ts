import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DashboardService, TenantDashboardResponse } from '../../../entities/dashboard';

export interface TenantDashboardState {
  status: 'idle' | 'loading' | 'success' | 'error';
  data: TenantDashboardResponse | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class TenantDashboardUseCase {
  private readonly dashboardService = inject(DashboardService);
  private readonly state = signal<TenantDashboardState>({ status: 'idle', data: null, error: null });

  readonly status = computed(() => this.state().status);
  readonly data = computed(() => this.state().data);
  readonly error = computed(() => this.state().error);

  async load(fromDate?: string, toDate?: string): Promise<void> {
    if (this.state().status === 'loading') return;
    this.state.set({ status: 'loading', data: this.state().data, error: null });
    try {
      const response = await firstValueFrom(this.dashboardService.getTenantDashboard(fromDate, toDate));
      if (response.success && response.data) {
        this.state.set({ status: 'success', data: response.data, error: null });
        return;
      }
      this.state.set({ status: 'error', data: null, error: response.message || 'No se pudo cargar el dashboard' });
    } catch (error: any) {
      this.state.set({
        status: 'error',
        data: null,
        error: error?.error?.message || error?.message || 'No se pudo conectar con el servidor'
      });
    }
  }
}
