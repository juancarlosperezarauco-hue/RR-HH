import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PlatformService } from '../../../entities/platform/api/platform.service';
import { SuperadminDashboardResponse } from '../../../entities/platform/api/platform-dashboard.model';

export interface PlatformDashboardState {
  status: 'idle' | 'loading' | 'success' | 'error';
  dashboard: SuperadminDashboardResponse | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformDashboardUseCase {
  private readonly platformService = inject(PlatformService);

  private readonly state = signal<PlatformDashboardState>({
    status: 'idle',
    dashboard: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly dashboard = computed(() => this.state().dashboard);
  readonly error = computed(() => this.state().error);

  async loadDashboard(): Promise<SuperadminDashboardResponse | null> {
    this.state.set({ status: 'loading', dashboard: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.getDashboardSummary());
      if (response.success && response.data) {
        this.state.set({ status: 'success', dashboard: response.data, error: null });
        return response.data;
      } else {
        this.state.set({ status: 'error', dashboard: null, error: response.message || 'Error al cargar el dashboard' });
        return null;
      }
    } catch (err: any) {
      this.state.set({ status: 'error', dashboard: null, error: err.message || 'Error al cargar el dashboard' });
      return null;
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', dashboard: null, error: null });
  }
}
