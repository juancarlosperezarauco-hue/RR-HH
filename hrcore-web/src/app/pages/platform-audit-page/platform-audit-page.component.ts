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
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Plataforma superadmin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Auditoría de plataforma
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Revisa eventos, actores y recursos auditados con una vista clara y navegable.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Recargar
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Eventos en página</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Cargados en la vista actual actual</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Actores</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().actors }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Sujetos únicos</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Recursos</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ stats().resources }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Tipos de recurso observados</p>
        </div>

        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Filtrados</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ filteredEvents().length }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Resultado visible en pantalla</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-1">
            <h2 class="text-lg font-bold text-accent-foreground">Filtros y búsqueda</h2>
            <p class="text-sm text-muted-foreground">Filtra por actor, evento, recurso o contenido de detalle.</p>
          </div>

          <div class="flex flex-col gap-3 xl:flex-row xl:items-center">
            <label class="flex min-w-[260px] items-center gap-3 rounded-2xl border border-border bg-muted px-4 py-3">
              <span class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Buscar</span>
              <input
                type="text"
                [value]="searchQuery()"
                (input)="setSearchQuery($any($event.target).value)"
                placeholder="Actor, evento, recurso o detalle"
                class="w-full border-0 bg-transparent text-sm text-accent-foreground outline-none placeholder:text-muted-foreground" />
            </label>

            <div class="flex flex-wrap gap-2">
              @for (filter of eventTypeFilters(); track filter.value) {
                <button
                  type="button"
                  (click)="setEventTypeFilter(filter.value)"
                  class="cursor-pointer rounded-full px-4 py-2 text-sm font-semibold transition-colors"
                  [class.bg-primary]="eventTypeFilter() === filter.value"
                  [class.text-primary-foreground]="eventTypeFilter() === filter.value"
                  [class.bg-accent]="eventTypeFilter() !== filter.value"
                  [class.text-primary]="eventTypeFilter() !== filter.value">
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
                  [class.bg-primary]="resourceTypeFilter() === filter.value"
                  [class.text-primary-foreground]="resourceTypeFilter() === filter.value"
                  [class.bg-accent]="resourceTypeFilter() !== filter.value"
                  [class.text-primary]="resourceTypeFilter() !== filter.value">
                  {{ filter.label }}
                </button>
              }
            </div>
          </div>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="text-lg font-bold text-accent-foreground">Eventos auditados</h2>
            <p class="text-sm text-muted-foreground">
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
          <div class="rounded-2xl border border-dashed border-border bg-muted px-4 py-8 text-sm text-muted-foreground">
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
                class="cursor-pointer rounded-full border border-border p-2 text-primary transition-colors hover:bg-accent">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="mt-6 grid gap-4 sm:grid-cols-2">
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Fecha</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ formatDate(event.createdAt) }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Actor</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.actorSubject }}</p>
                <p class="mt-1 break-words text-xs text-muted-foreground">{{ event.actorEmail || 'Sin correo' }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Evento</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.eventType }}</p>
                <p class="mt-1 break-words text-xs text-muted-foreground">{{ event.tenantSlug || 'Sin tenant' }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Recurso</p>
                <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">
                  {{ event.resourceType }}
                </p>
                <p class="mt-1 break-words text-xs text-muted-foreground">{{ event.resourceId }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4 sm:col-span-2">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Origen</p>
                <div class="mt-2 grid gap-4 sm:grid-cols-2">
                  <div class="rounded-2xl border border-border bg-card p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">IP</p>
                    <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.ipAddress || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-border bg-card p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">User Agent</p>
                    <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.userAgent || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-border bg-card p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Source</p>
                    <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.source || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-border bg-card p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Resultado</p>
                    <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.outcome || 'SUCCESS' }}</p>
                  </div>
                </div>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4 sm:col-span-2">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Trazabilidad</p>
                <div class="mt-2 grid gap-4 sm:grid-cols-2">
                  <div class="rounded-2xl border border-border bg-card p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Request ID</p>
                    <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.requestId || 'N/A' }}</p>
                  </div>
                  <div class="rounded-2xl border border-border bg-card p-4">
                    <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Correlation ID</p>
                    <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ event.correlationId || 'N/A' }}</p>
                  </div>
                </div>
              </div>
              <div class="rounded-2xl border border-border bg-muted p-4 sm:col-span-2">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Detalle</p>
                @if (parsedDetails()) {
                  <div class="mt-3 grid gap-4 sm:grid-cols-2">
                    @for (item of parsedDetailsEntries(); track item.key) {
                      <div class="rounded-2xl border border-border bg-card p-4">
                        <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">{{ item.key }}</p>
                        <p class="mt-1 break-words text-sm font-semibold text-accent-foreground">{{ item.value }}</p>
                      </div>
                    }
                    @if (hasMoreParsedDetails()) {
                      <div class="rounded-2xl border border-dashed border-border bg-card p-4 sm:col-span-2">
                        <p class="text-xs font-bold uppercase tracking-[0.12em] text-muted-foreground">Más datos</p>
                        <p class="mt-1 text-sm font-semibold text-accent-foreground">
                          El payload tiene más campos, pero solo se muestran los más relevantes.
                        </p>
                      </div>
                    }
                  </div>
                } @else {
                  <pre class="mt-3 overflow-x-auto whitespace-pre-wrap break-words rounded-2xl border border-border bg-card p-4 text-xs leading-6 text-accent-foreground">{{ event.eventDetails }}</pre>
                }
              </div>
            </div>

            <div class="app-modal-footer">
              <button
                type="button"
                (click)="closeModal()"
                class="cursor-pointer rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90">
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
