// features/platform/application/platform-plan-list.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { PageResponse, PlatformService, PlatformPlan } from '../../../entities/platform/api/platform.service';

export interface PlatformPlanListState {
  status: 'idle' | 'loading' | 'success' | 'error';
  plans: PlatformPlan[];
  page: PageResponse<PlatformPlan> | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformPlanListUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformPlanListState>({
    status: 'idle',
    plans: [],
    page: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly plans = computed(() => this.state().plans);
  readonly page = computed(() => this.state().page);
  readonly totalElements = computed(() => this.state().page?.totalElements ?? 0);
  readonly totalPages = computed(() => this.state().page?.totalPages ?? 0);
  readonly currentPage = computed(() => this.state().page?.number ?? 0);
  readonly error = computed(() => this.state().error);

  async loadPlans(page = 0, size = 20): Promise<void> {
    this.state.set({ status: 'loading', plans: [], page: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.getPlans(page, size).pipe(timeout(15000)));
      if (response.success && response.data) {
        this.state.set({
          status: 'success',
          plans: response.data.content,
          page: response.data,
          error: null
        });
      } else {
        this.state.set({ status: 'error', plans: [], page: null, error: response.message });
      }
    } catch (err: any) {
      this.state.set({
        status: 'error',
        plans: [],
        page: null,
        error: err?.message || 'Error al cargar planes'
      });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', plans: [], page: null, error: null });
  }
}
