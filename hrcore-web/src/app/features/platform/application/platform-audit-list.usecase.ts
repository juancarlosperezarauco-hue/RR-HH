// features/platform/application/platform-audit-list.usecase.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { AuditEvent, PageResponse, PlatformService } from '../../../entities/platform/api/platform.service';

export interface PlatformAuditListState {
  status: 'idle' | 'loading' | 'success' | 'error';
  events: AuditEvent[];
  page: PageResponse<AuditEvent> | null;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class PlatformAuditListUseCase {
  private platformService = inject(PlatformService);

  private state = signal<PlatformAuditListState>({
    status: 'idle',
    events: [],
    page: null,
    error: null
  });

  readonly status = computed(() => this.state().status);
  readonly events = computed(() => this.state().events);
  readonly page = computed(() => this.state().page);
  readonly totalElements = computed(() => this.state().page?.totalElements ?? 0);
  readonly totalPages = computed(() => this.state().page?.totalPages ?? 0);
  readonly currentPage = computed(() => this.state().page?.number ?? 0);
  readonly error = computed(() => this.state().error);

  async loadAuditEvents(page = 0, size = 20): Promise<void> {
    this.state.set({ status: 'loading', events: [], page: null, error: null });

    try {
      const response = await firstValueFrom(this.platformService.getAuditEvents(page, size).pipe(timeout(15000)));
      if (response.success && response.data) {
        this.state.set({
          status: 'success',
          events: response.data.content,
          page: response.data,
          error: null
        });
      } else {
        this.state.set({ status: 'error', events: [], page: null, error: response.message });
      }
    } catch (err: any) {
      this.state.set({
        status: 'error',
        events: [],
        page: null,
        error: err?.message || 'Error al cargar auditoría'
      });
    }
  }

  resetState(): void {
    this.state.set({ status: 'idle', events: [], page: null, error: null });
  }
}
