import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { PageResponse, PlatformService, PlatformTenant } from '../../../entities/platform/api/platform.service';

export interface PlatformTenantListState {
  status: 'idle' | 'loading' | 'success' | 'error';
  tenants: PlatformTenant[];
  page: PageResponse<PlatformTenant> | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformTenantListUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformTenantListState>({
    status: 'idle',
    tenants: [],
    page: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly tenants = computed(() => this.state().tenants);
  readonly page = computed(() => this.state().page);
  readonly totalElements = computed(() => this.state().page?.totalElements ?? 0);
  readonly totalPages = computed(() => this.state().page?.totalPages ?? 0);
  readonly currentPage = computed(() => this.state().page?.number ?? 0);
  readonly error = computed(() => this.state().error);

  async loadTenants(page = 0, size = 20): Promise<void> {
    this.state.set({ status: 'loading', tenants: [], page: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.getTenants(page, size).pipe(timeout(15000)));
      if (response.success && response.data) {
        this.state.set({
          status: 'success',
          tenants: response.data.content,
          page: response.data,
          error: null
        });
      } else {
        this.state.set({ status: 'error', tenants: [], page: null, error: response.message });
      }
    } catch (err: any) {
      this.state.set({
        status: 'error',
        tenants: [],
        page: null,
        error: err?.message || 'Error al cargar tenants'
      });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', tenants: [], page: null, error: null });
  }
}
