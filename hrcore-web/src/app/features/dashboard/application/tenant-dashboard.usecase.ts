import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DashboardService, TenantDashboardResponse } from '../../../entities/dashboard';
import { AuthFacade } from '../../../shared/lib/auth/auth.facade';

export interface TenantDashboardState {
  status: 'idle' | 'loading' | 'success' | 'error';
  data: TenantDashboardResponse | null;
  error: string | null;
  notice: string | null;
}

@Injectable({ providedIn: 'root' })
export class TenantDashboardUseCase {
  private readonly dashboardService = inject(DashboardService);
  private readonly auth = inject(AuthFacade);
  private readonly state = signal<TenantDashboardState>({ status: 'idle', data: null, error: null, notice: null });

  readonly status = computed(() => this.state().status);
  readonly data = computed(() => this.state().data);
  readonly error = computed(() => this.state().error);
  readonly notice = computed(() => this.state().notice);

  async load(fromDate?: string, toDate?: string): Promise<void> {
    if (this.state().status === 'loading') return;
    this.state.set({ status: 'loading', data: this.state().data, error: null, notice: null });
    try {
      const response = await firstValueFrom(this.dashboardService.getTenantDashboard(fromDate, toDate));
      if (response.success && response.data) {
        this.state.set({ status: 'success', data: response.data, error: null, notice: null });
        return;
      }
      if (this.isPermissionError(response.message)) {
        this.setLimitedAccess(fromDate, toDate);
        return;
      }
      this.state.set({ status: 'error', data: null, error: response.message || 'No se pudo cargar el dashboard', notice: null });
    } catch (error: any) {
      if (error?.status === 403) {
        this.setLimitedAccess(fromDate, toDate);
        return;
      }
      this.state.set({
        status: 'error',
        data: null,
        error: error?.error?.message || error?.message || 'No se pudo conectar con el servidor',
        notice: null
      });
    }
  }

  private setLimitedAccess(fromDate?: string, toDate?: string): void {
    const today = new Date();
    const fallbackTo = today.toISOString().slice(0, 10);
    const fallbackFromDate = new Date(today);
    fallbackFromDate.setDate(today.getDate() - 30);
    const fallbackFrom = fallbackFromDate.toISOString().slice(0, 10);

    this.state.set({
      status: 'success',
      data: {
        metadata: {
          generatedAt: today.toISOString(),
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
          tenantSlug: this.auth.currentUser()?.tenantSlug || '',
          generatedBy: null,
          fromDate: fromDate || fallbackFrom,
          toDate: toDate || fallbackTo
        },
        metrics: [],
        sections: []
      },
      error: null,
      notice: 'No tienes permiso para consultar los indicadores del dashboard. Puedes continuar usando los módulos habilitados para tu cuenta.'
    });
  }

  private isPermissionError(message: string | null | undefined): boolean {
    const normalized = (message || '').toLowerCase();
    return normalized.includes('permiso') || normalized.includes('acceso denegado') || normalized.includes('forbidden');
  }
}
