import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { PlatformAuditListUseCase } from '../../features/platform/application/platform-audit-list.usecase';
import { PlatformAuditTableComponent } from '../../features/platform/ui/platform-audit-table/platform-audit-table.component';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { AuditEvent } from '../../entities/platform/api/platform.service';

@Component({
  selector: 'app-platform-audit-page',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PlatformAuditTableComponent, PlatformPaginationComponent],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              Plataforma superadmin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Auditoría de plataforma
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Revisa eventos, actores y recursos auditados con una vista clara y navegable.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Recargar
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Eventos en página</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Cargados en la vista actual actual</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Actores</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().actors }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Sujetos únicos</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Recursos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().resources }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Tipos de recurso observados</p>
        </div>

        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Filtrados</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ filteredEvents().length }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Resultado visible en pantalla</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-1">
            <h2 class="text-lg font-bold text-[#1B5E20]">Filtros y búsqueda</h2>
            <p class="text-sm text-[#6B7D6C]">Filtra por actor, evento, recurso o contenido de detalle.</p>
          </div>

          <div class="flex flex-col gap-3 xl:flex-row xl:items-center">
            <label class="flex min-w-[260px] items-center gap-3 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3">
              <span class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Buscar</span>
              <input
                type="text"
                [value]="searchQuery()"
                (input)="setSearchQuery($any($event.target).value)"
                placeholder="Actor, evento, recurso o detalle"
                class="w-full border-0 bg-transparent text-sm text-[#1B5E20] outline-none placeholder:text-[#9AA99A]" />
            </label>

            <div class="flex flex-wrap gap-2">
              @for (filter of eventTypeFilters(); track filter.value) {
                <button
                  type="button"
                  (click)="setEventTypeFilter(filter.value)"
                  class="cursor-pointer rounded-full px-4 py-2 text-sm font-semibold transition-colors"
                  [class.bg-[#2E7D32]]="eventTypeFilter() === filter.value"
                  [class.text-white]="eventTypeFilter() === filter.value"
                  [class.bg-[#F1F8E9]]="eventTypeFilter() !== filter.value"
                  [class.text-[#2E7D32]]="eventTypeFilter() !== filter.value">
                  {{ filter.label }}
                </button>
              }
            </div>

            <div class="flex flex-wrap gap-2">
              @for (filter of resourceTypeFilters(); track filter.value) {
                <button
                  type="button"
                  (click)="setResourceTypeFilter(filter.value)"
                  class="cursor-pointer rounded-full px-4 py-2 text-sm font-semibold transition-colors"
                  [class.bg-[#2E7D32]]="resourceTypeFilter() === filter.value"
                  [class.text-white]="resourceTypeFilter() === filter.value"
                  [class.bg-[#F1F8E9]]="resourceTypeFilter() !== filter.value"
                  [class.text-[#2E7D32]]="resourceTypeFilter() !== filter.value">
                  {{ filter.label }}
                </button>
              }
            </div>
          </div>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-bold text-[#1B5E20]">Eventos auditados</h2>
            <p class="text-sm text-[#6B7D6C]">
              {{ filteredEvents().length }} resultado(s) de {{ stats().total }} evento(s)
            </p>
          </div>
        </div>

        <app-platform-audit-table
          [events]="filteredEvents()"
          [isLoading]="isLoading"
          (viewDetails)="viewDetails($event)">
        </app-platform-audit-table>

        @if (listUseCase.page(); as page) {
          <div class="mt-4">
            <app-platform-pagination
              [currentPage]="page.number"
              [totalPages]="page.totalPages"
              [totalElements]="page.totalElements"
              [isLoading]="isLoading"
              (pageChange)="changePage($event)">
            </app-platform-pagination>
          </div>
        }

        @if (!isLoading && filteredEvents().length === 0) {
          <div class="rounded-2xl border border-dashed border-[#C8E6C9] bg-[#FAFCF8] px-4 py-8 text-sm text-[#6B7D6C]">
            No hay eventos de auditoría que coincidan con los filtros actuales.
          </div>
        }
      </section>

      @if (selectedEvent(); as event) {
        <div class="app-modal-overlay" (click)="closeModal()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Detalle del evento</h2>
                <p class="app-modal-subtitle">Información completa del registro auditado seleccionado.</p>
              </div>
              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="mt-6 grid gap-4 sm:grid-cols-2">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Fecha</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ formatDate(event.createdAt) }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Actor</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.actorSubject }}</p>
                <p class="mt-1 break-words text-xs text-[#6B7D6C]">{{ event.actorEmail || 'Sin correo' }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Evento</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.eventType }}</p>
                <p class="mt-1 break-words text-xs text-[#6B7D6C]">{{ event.tenantSlug || 'Sin tenant' }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Recurso</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">
                  {{ event.resourceType }}
                </p>
                <p class="mt-1 break-words text-xs text-[#6B7D6C]">{{ event.resourceId }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 sm:col-span-2">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Origen</p>
                <div class="mt-2 grid gap-4 sm:grid-cols-2">
                  <div class="rounded-2xl border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">IP</p>
                    <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.ipAddress || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">User Agent</p>
                    <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.userAgent || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Source</p>
                    <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.source || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Resultado</p>
                    <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.outcome || 'SUCCESS' }}</p>
                  </div>
                </div>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 sm:col-span-2">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Trazabilidad</p>
                <div class="mt-2 grid gap-4 sm:grid-cols-2">
                  <div class="rounded-2xl border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Request ID</p>
                    <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.requestId || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Correlation ID</p>
                    <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ event.correlationId || 'N/A' }}</p>
                  </div>
                </div>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 sm:col-span-2">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Detalle</p>
                @if (parsedDetails()) {
                  <div class="mt-3 grid gap-4 sm:grid-cols-2">
                    @for (item of parsedDetailsEntries(); track item.key) {
                      <div class="rounded-2xl border border-[#DDEED8] bg-white p-4">
                        <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">{{ item.key }}</p>
                        <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ item.value }}</p>
                      </div>
                    }
                    @if (hasMoreParsedDetails()) {
                      <div class="rounded-2xl border border-dashed border-[#DDEED8] bg-white p-4 sm:col-span-2">
                        <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Más datos</p>
                        <p class="mt-1 text-sm font-semibold text-[#1B5E20]">
                          El payload tiene más campos, pero solo se muestran los más relevantes.
                        </p>
                      </div>
                    }
                  </div>
                } @else {
                  <pre class="mt-3 overflow-x-auto whitespace-pre-wrap break-words rounded-2xl border border-[#DDEED8] bg-white p-4 text-xs leading-6 text-[#1B5E20]">{{ event.eventDetails }}</pre>
                }
              </div>
            </div>

            <div class="app-modal-footer">
              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full bg-[#2E7D32] px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#256428]">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class PlatformAuditPageComponent implements OnInit {
  protected readonly listUseCase = inject(PlatformAuditListUseCase);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly pageSize = 20;

  readonly searchQuery = signal('');
  readonly eventTypeFilter = signal<'all' | string>('all');
  readonly resourceTypeFilter = signal<'all' | string>('all');
  readonly selectedEvent = signal<AuditEvent | null>(null);

  readonly stats = computed(() => {
    const events = this.listUseCase.events();
    const actors = new Set(events.map((event) => event.actorSubject)).size;
    const resources = new Set(events.map((event) => event.resourceType)).size;
    return {
      total: events.length,
      actors,
      resources
    };
  });

  readonly filteredEvents = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const eventTypeFilter = this.eventTypeFilter();
    const resourceTypeFilter = this.resourceTypeFilter();

    return [...this.listUseCase.events()]
      .filter((event) => {
        const haystack = [
          event.actorSubject,
          event.eventType,
          event.resourceType,
          event.resourceId,
          event.eventDetails
        ]
          .join(' ')
          .toLowerCase();

        const matchesQuery = !query || haystack.includes(query);
        const matchesEventType = eventTypeFilter === 'all' ? true : event.eventType === eventTypeFilter;
        const matchesResourceType = resourceTypeFilter === 'all' ? true : event.resourceType === resourceTypeFilter;
        return matchesQuery && matchesEventType && matchesResourceType;
      })
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt));
  });

  readonly parsedDetails = computed(() => this.parseEventDetails(this.selectedEvent()?.eventDetails ?? null));
  readonly parsedDetailsEntries = computed(() =>
    Object.entries(this.parsedDetails() ?? {})
      .slice(0, 6)
      .map(([key, value]) => ({
        key,
        value: this.stringifyDetailValue(value)
      }))
  );
  readonly hasMoreParsedDetails = computed(() => {
    const details = this.parsedDetails();
    return !!details && Object.keys(details).length > 6;
  });

  readonly eventTypeFilters = computed(() => {
    const values = [...new Set(this.listUseCase.events().map((event) => event.eventType))].sort();
    return [{ label: 'Todos', value: 'all' }, ...values.map((value) => ({ label: value, value }))];
  });

  readonly resourceTypeFilters = computed(() => {
    const values = [...new Set(this.listUseCase.events().map((event) => event.resourceType))].sort();
    return [{ label: 'Todos', value: 'all' }, ...values.map((value) => ({ label: value, value }))];
  });

  get isLoading(): boolean {
    return this.listUseCase.status() === 'loading';
  }

  ngOnInit(): void {
    void this.reload();
  }

  async reload(page = this.listUseCase.currentPage()): Promise<void> {
    await this.listUseCase.loadAuditEvents(page, this.pageSize);
    this.cdr.detectChanges();
  }

  async changePage(page: number): Promise<void> {
    await this.reload(page);
    this.cdr.detectChanges();
  }

  setSearchQuery(value: string): void {
    this.searchQuery.set(value);
  }

  setEventTypeFilter(value: 'all' | string): void {
    this.eventTypeFilter.set(value);
  }

  setResourceTypeFilter(value: 'all' | string): void {
    this.resourceTypeFilter.set(value);
  }

  viewDetails(event: AuditEvent): void {
    this.selectedEvent.set(event);
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.selectedEvent.set(null);
    this.cdr.detectChanges();
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'N/A';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleString('es-BO', {
      dateStyle: 'medium',
      timeStyle: 'short'
    });
  }

  private parseEventDetails(raw: string | null): Record<string, unknown> | null {
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return parsed as Record<string, unknown>;
      }
      return null;
    } catch {
      return null;
    }
  }

  private stringifyDetailValue(value: unknown): string {
    if (value === null || value === undefined) {
      return 'N/A';
    }

    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      return String(value);
    }

    return JSON.stringify(value);
  }
}
