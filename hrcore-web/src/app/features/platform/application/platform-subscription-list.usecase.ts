// features/platform/application/platform-subscription-list.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { PageResponse, PlatformService, PlatformSubscription } from '../../../entities/platform/api/platform.service';

export interface PlatformSubscriptionListState {
  status: 'idle' | 'loading' | 'success' | 'error';
  subscriptions: PlatformSubscription[];
  page: PageResponse<PlatformSubscription> | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformSubscriptionListUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformSubscriptionListState>({
    status: 'idle',
    subscriptions: [],
    page: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly subscriptions = computed(() => this.state().subscriptions);
  readonly page = computed(() => this.state().page);
  readonly totalElements = computed(() => this.state().page?.totalElements ?? 0);
  readonly totalPages = computed(() => this.state().page?.totalPages ?? 0);
  readonly currentPage = computed(() => this.state().page?.number ?? 0);
  readonly error = computed(() => this.state().error);

  async loadSubscriptions(page = 0, size = 20): Promise<void> {
    this.state.set({ status: 'loading', subscriptions: [], page: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.getSubscriptions(page, size).pipe(timeout(15000)));
      if (response.success && response.data) {
        this.state.set({
          status: 'success',
          subscriptions: response.data.content,
          page: response.data,
          error: null
        });
      } else {
        this.state.set({ status: 'error', subscriptions: [], page: null, error: response.message });
      }
    } catch (err: any) {
      this.state.set({
        status: 'error',
        subscriptions: [],
        page: null,
        error: err?.message || 'Error al cargar suscripciones'
      });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', subscriptions: [], page: null, error: null });
  }
}
