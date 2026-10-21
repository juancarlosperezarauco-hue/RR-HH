import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, timeout } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { TenantAuditService, TenantAuditEventResponse, TenantAuditFilters } from '../../entities/audit';
import { PageResponse } from '../../shared/api';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';

@Component({
  selector: 'app-tenant-audit-page',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, PlatformPaginationComponent],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <!-- Encabezado -->
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Historial de actividades
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Bitácora de auditoría
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Supervisa las operaciones realizadas, cambios relevantes y accesos dentro de tu organización de forma inmutable.
              </p>
            </div>
          </div>
          <button
            type="button"
            (click)="reload()"
            [disabled]="isLoading()"
            class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50">
            <lucide-icon name="refresh-ccw" class="h-4 w-4" [class.animate-spin]="isLoading()"></lucide-icon>
            Recargar
          </button>
        </div>
      </section>

      <!-- Métricas rápidas -->
      <section class="grid gap-4 sm:grid-cols-3">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Total eventos</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ totalElements() }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Registros en el historial</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Usuarios ejecutores</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ actorCount() }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Actores en página actual</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Tipos de acción</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ eventTypeCount() }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Operaciones registradas</p>
        </div>
      </section>

      <!-- Panel de Filtros -->
      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm space-y-4">
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-b border-border pb-4">
          <div>
            <h2 class="text-lg font-bold text-accent-foreground">Filtros de búsqueda</h2>
            <p class="text-sm text-muted-foreground">Filtra actividades por fechas, usuario ejecutor, módulo o tipo de recurso.</p>
          </div>
          <button
            type="button"
            (click)="resetFilters()"
            class="text-xs font-semibold text-primary hover:underline cursor-pointer flex items-center gap-1 self-start sm:self-auto">
            <lucide-icon name="rotate-ccw" class="h-3.5 w-3.5"></lucide-icon>
            Limpiar filtros
          </button>
        </div>

        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          <!-- Búsqueda libre -->
          <div class="space-y-1">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Búsqueda</label>
            <input
              type="text"
              [ngModel]="searchFilter()"
              (ngModelChange)="searchFilter.set($event); applyFilters()"
              placeholder="Buscar término..."
              class="h-10 w-full rounded-xl border border-border bg-muted px-3 text-xs text-accent-foreground outline-none focus:border-primary focus:bg-card" />
          </div>

          <!-- Fecha Desde -->
          <div class="space-y-1">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Fecha desde</label>
            <input
              type="date"
              [ngModel]="startDateFilter()"
              (ngModelChange)="startDateFilter.set($event); applyFilters()"
              class="h-10 w-full rounded-xl border border-border bg-muted px-3 text-xs text-accent-foreground outline-none focus:border-primary focus:bg-card" />
          </div>

          <!-- Fecha Hasta -->
          <div class="space-y-1">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Fecha hasta</label>
            <input
              type="date"
              [ngModel]="endDateFilter()"
              (ngModelChange)="endDateFilter.set($event); applyFilters()"
              class="h-10 w-full rounded-xl border border-border bg-muted px-3 text-xs text-accent-foreground outline-none focus:border-primary focus:bg-card" />
          </div>

          <!-- Usuario ejecutor -->
          <div class="space-y-1">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Usuario ejecutor</label>
            <input
              type="text"
              [ngModel]="actorFilter()"
              (ngModelChange)="actorFilter.set($event); applyFilters()"
              placeholder="Correo o nombre..."
              class="h-10 w-full rounded-xl border border-border bg-muted px-3 text-xs text-accent-foreground outline-none focus:border-primary focus:bg-card" />
          </div>

          <!-- Módulo -->
          <div class="space-y-1">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Módulo</label>
            <select
              [ngModel]="moduleFilter()"
              (ngModelChange)="moduleFilter.set($event); applyFilters()"
              class="h-10 w-full rounded-xl border border-border bg-muted px-3 text-xs text-accent-foreground outline-none focus:border-primary focus:bg-card">
              <option value="all">Todos los módulos</option>
              <option value="USER">Usuarios y accesos</option>
              <option value="ROLE">Roles y permisos</option>
              <option value="EMPLOYEE">Empleados</option>
              <option value="CONTRACT">Contratos</option>
              <option value="SHIFT">Turnos de trabajo</option>
              <option value="ATTENDANCE">Asistencia</option>
              <option value="LEAVE">Ausencias y licencias</option>
              <option value="INCIDENT">Incidencias</option>
              <option value="BACKUP">Respaldos</option>
              <option value="TENANT_SETTINGS">Configuración empresa</option>
            </select>
          </div>

          <!-- Recurso afectado -->
          <div class="space-y-1">
            <label class="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Recurso afectado</label>
            <select
              [ngModel]="resourceTypeFilter()"
              (ngModelChange)="resourceTypeFilter.set($event); applyFilters()"
              class="h-10 w-full rounded-xl border border-border bg-muted px-3 text-xs text-accent-foreground outline-none focus:border-primary focus:bg-card">
              <option value="all">Todos los recursos</option>
              <option value="USER">USER</option>
              <option value="ROLE">ROLE</option>
              <option value="EMPLOYEE">EMPLOYEE</option>
              <option value="CONTRACT">CONTRACT</option>
              <option value="EMPLOYEE_DOCUMENT">EMPLOYEE_DOCUMENT</option>
              <option value="SHIFT">SHIFT</option>
              <option value="ATTENDANCE_RECORD">ATTENDANCE_RECORD</option>
              <option value="ATTENDANCE_ADJUSTMENT">ATTENDANCE_ADJUSTMENT</option>
              <option value="BACKUP">BACKUP</option>
              <option value="TENANT_SETTINGS">TENANT_SETTINGS</option>
            </select>
          </div>
        </div>
      </section>

      <!-- Tabla de Eventos -->
      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm space-y-4">
        @if (isLoading() && events().length === 0) {
          <div class="flex flex-col items-center justify-center gap-4 p-12 text-muted-foreground">
            <svg class="h-8 w-8 animate-spin text-primary" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p>Cargando historial de auditoría...</p>
          </div>
        } @else {
          <div class="overflow-x-auto rounded-2xl border border-border">
            <table class="min-w-[900px] w-full divide-y divide-border">
              <thead class="bg-accent">
                <tr class="text-left text-xs font-bold uppercase tracking-[0.12em] text-primary">
                  <th class="px-4 py-3">Fecha y hora</th>
                  <th class="px-4 py-3">Usuario ejecutor</th>
                  <th class="px-4 py-3">Acción / Evento</th>
                  <th class="px-4 py-3">Recurso afectado</th>
                  <th class="px-4 py-3">Dirección IP</th>
                  <th class="px-4 py-3 text-center">Resultado</th>
                  <th class="px-4 py-3 text-right">Detalle</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-border bg-card">
                @for (event of events(); track event.id) {
                  <tr class="hover:bg-accent/50 transition-colors">
                    <td class="whitespace-nowrap px-4 py-3.5 text-xs text-accent-foreground font-mono">
                      {{ formatDate(event.createdAt) }}
                    </td>
                    <td class="px-4 py-3.5">
                      <div class="text-sm font-bold text-accent-foreground">
                        {{ getActorDisplayName(event) }}
                      </div>
                      <div class="text-xs text-muted-foreground">
                        {{ event.actorEmail || 'Sistema' }}
                      </div>
                    </td>
                    <td class="px-4 py-3.5">
                      <div class="text-xs font-bold text-primary">
                        {{ getEventActionLabel(event.eventType) }}
                      </div>
                      <div class="text-[10px] text-muted-foreground font-mono">
                        {{ event.eventType }}
                      </div>
                    </td>
                    <td class="px-4 py-3.5">
                      <div class="text-xs font-bold text-accent-foreground">
                        {{ getResourceLabel(event.resourceType) }}
                      </div>
                      <div class="text-[11px] text-muted-foreground font-mono truncate max-w-[140px]" [title]="event.resourceId">
                        {{ event.resourceId }}
                      </div>
                    </td>
                    <td class="px-4 py-3.5 text-xs text-muted-foreground font-mono">
                      {{ event.ipAddress || '—' }}
                    </td>
                    <td class="px-4 py-3.5 text-center">
                      <span
                        class="inline-flex rounded-full px-2.5 py-0.5 text-[11px] font-bold tracking-wider"
                        [class.bg-emerald-500/10]="(event.outcome || 'SUCCESS') === 'SUCCESS'"
                        [class.text-emerald-600]="(event.outcome || 'SUCCESS') === 'SUCCESS'"
                        [class.bg-destructive/10]="(event.outcome || 'SUCCESS') !== 'SUCCESS'"
                        [class.text-destructive]="(event.outcome || 'SUCCESS') !== 'SUCCESS'">
                        {{ getOutcomeLabel(event.outcome) }}
                      </span>
                    </td>
                    <td class="px-4 py-3.5 text-right">
                      <button
                        type="button"
                        (click)="openDetailModal(event)"
                        class="cursor-pointer inline-flex items-center gap-1 rounded-full border border-border px-3 py-1.5 text-xs font-semibold text-primary hover:bg-accent transition-colors"
                        title="Ver detalle del evento">
                        <lucide-icon name="eye" [size]="14"></lucide-icon>
                        Ver detalle
                      </button>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="7" class="px-4 py-12 text-center text-muted-foreground">
                      No se encontraron eventos de auditoría con los criterios especificados.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <app-platform-pagination
            class="block"
            [currentPage]="currentPage()"
            [totalPages]="totalPages()"
            [totalElements]="totalElements()"
            [isLoading]="isLoading()"
            (pageChange)="onPageChange($event)">
          </app-platform-pagination>
        }
      </section>
    </div>

    <!-- Modal de Detalle de Evento (Criterio 4) -->
    @if (selectedEvent()) {
      <div class="app-modal-overlay">
        <div class="w-full max-w-3xl rounded-[28px] border border-border bg-card shadow-2xl overflow-hidden flex flex-col max-h-[calc(100vh-2rem)] sm:max-h-[85vh]">
          <!-- Encabezado fijo -->
          <div class="shrink-0 flex items-start justify-between gap-4 border-b border-border p-6 bg-card">
            <div>
              <div class="flex items-center gap-2">
                <span class="inline-flex rounded-full bg-primary/10 border border-primary/20 px-2.5 py-0.5 text-xs font-bold text-primary">
                  {{ getEventActionLabel(selectedEvent()!.eventType) }}
                </span>
                <span class="text-xs text-muted-foreground font-mono">ID: {{ selectedEvent()!.id }}</span>
              </div>
              <h2 class="text-2xl font-black text-primary mt-2">Detalle del evento de auditoría</h2>
              <p class="mt-1 text-sm text-muted-foreground">Registro cronológico e inmutable de la operación ejecutada.</p>
            </div>
            <button
              type="button"
              (click)="closeDetailModal()"
              class="cursor-pointer rounded-full border border-border p-2 text-primary transition-colors hover:bg-accent shrink-0">
              <lucide-icon name="x" [size]="20"></lucide-icon>
            </button>
          </div>

          <!-- Contenido scrolleable único -->
          <div class="flex-1 overflow-y-auto p-6 space-y-6">
            <!-- Bloque 1: General y Marca de Tiempo -->
            <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div class="rounded-2xl border border-border bg-muted/40 p-4">
                <p class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Marca de tiempo</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground font-mono">
                  {{ formatFullDate(selectedEvent()!.createdAt) }}
                </p>
              </div>

              <div class="rounded-2xl border border-border bg-muted/40 p-4">
                <p class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Usuario ejecutor</p>
                <p class="mt-1 text-sm font-bold text-accent-foreground truncate">
                  {{ getActorDisplayName(selectedEvent()!) }}
                </p>
                <p class="text-xs text-muted-foreground truncate">{{ selectedEvent()!.actorEmail || 'Sistema' }}</p>
              </div>

              <div class="rounded-2xl border border-border bg-muted/40 p-4">
                <p class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Resultado</p>
                <p class="mt-1 text-sm font-bold"
                  [class.text-emerald-600]="(selectedEvent()!.outcome || 'SUCCESS') === 'SUCCESS'"
                  [class.text-destructive]="(selectedEvent()!.outcome || 'SUCCESS') !== 'SUCCESS'">
                  {{ getOutcomeLabel(selectedEvent()!.outcome) }}
                </p>
              </div>
            </div>

            <!-- Bloque 2: Entidad y Recurso Afectado -->
            <div class="rounded-2xl border border-border bg-card p-4 space-y-3">
              <h4 class="text-xs font-bold uppercase tracking-wider text-muted-foreground border-b border-border pb-2">
                Entidad afectada
              </h4>
              <div class="grid gap-4 sm:grid-cols-2">
                <div>
                  <span class="text-xs text-muted-foreground">Tipo de recurso (resourceType):</span>
                  <p class="text-sm font-bold text-accent-foreground">{{ getResourceLabel(selectedEvent()!.resourceType) }} ({{ selectedEvent()!.resourceType }})</p>
                </div>
                <div>
                  <span class="text-xs text-muted-foreground">Identificador (resourceId):</span>
                  <p class="text-sm font-semibold text-accent-foreground font-mono break-all">{{ selectedEvent()!.resourceId }}</p>
                </div>
              </div>
            </div>

            <!-- Bloque 3: Conexión y Trazabilidad (IP, User Agent, RequestId) -->
            <div class="rounded-2xl border border-border bg-card p-4 space-y-3">
              <h4 class="text-xs font-bold uppercase tracking-wider text-muted-foreground border-b border-border pb-2">
                Conexión y trazabilidad
              </h4>
              <div class="grid gap-4 sm:grid-cols-2">
                <div>
                  <span class="text-xs text-muted-foreground">Dirección IP (ipAddress):</span>
                  <p class="text-sm font-semibold text-accent-foreground font-mono">{{ selectedEvent()!.ipAddress || 'N/A' }}</p>
                </div>
                <div>
                  <span class="text-xs text-muted-foreground">Origen (source):</span>
                  <p class="text-sm font-semibold text-accent-foreground">{{ selectedEvent()!.source || 'APPLICATION' }}</p>
                </div>
                <div class="sm:col-span-2">
                  <span class="text-xs text-muted-foreground">Agente de usuario (userAgent):</span>
                  <p class="text-xs text-accent-foreground font-mono break-words bg-muted/60 p-2 rounded-xl mt-1">
                    {{ selectedEvent()!.userAgent || 'N/A' }}
                  </p>
                </div>
                <div>
                  <span class="text-xs text-muted-foreground">Request ID:</span>
                  <p class="text-xs text-accent-foreground font-mono break-all">{{ selectedEvent()!.requestId || 'N/A' }}</p>
                </div>
                <div>
                  <span class="text-xs text-muted-foreground">Correlation ID:</span>
                  <p class="text-xs text-accent-foreground font-mono break-all">{{ selectedEvent()!.correlationId || 'N/A' }}</p>
                </div>
              </div>
            </div>

            <!-- Bloque 4: Metadatos del cambio (metadata / eventDetails) -->
            <div class="rounded-2xl border border-border bg-card p-4 space-y-3">
              <h4 class="text-xs font-bold uppercase tracking-wider text-muted-foreground border-b border-border pb-2">
                Metadatos del cambio realizado (metadata)
              </h4>
              @if (selectedEvent()!.eventDetails) {
                <pre class="overflow-x-auto whitespace-pre-wrap break-words rounded-xl border border-border bg-muted/60 p-3 text-xs leading-5 font-mono text-accent-foreground">{{ formatJson(selectedEvent()!.eventDetails) }}</pre>
              } @else {
                <p class="text-xs text-muted-foreground italic">Sin metadatos adicionales registrados.</p>
              }

              @if (selectedEvent()!.beforeState || selectedEvent()!.afterState) {
                <div class="grid gap-4 sm:grid-cols-2 mt-4 pt-3 border-t border-border">
                  <div>
                    <span class="text-xs font-semibold text-muted-foreground uppercase">Estado previo (beforeState):</span>
                    <pre class="mt-1 overflow-x-auto whitespace-pre-wrap break-words rounded-xl border border-border bg-muted/60 p-3 text-xs leading-5 font-mono text-accent-foreground">{{ formatJson(selectedEvent()!.beforeState) }}</pre>
                  </div>
                  <div>
                    <span class="text-xs font-semibold text-muted-foreground uppercase">Estado resultante (afterState):</span>
                    <pre class="mt-1 overflow-x-auto whitespace-pre-wrap break-words rounded-xl border border-border bg-muted/60 p-3 text-xs leading-5 font-mono text-accent-foreground">{{ formatJson(selectedEvent()!.afterState) }}</pre>
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      </div>
    }
  `
})
export class TenantAuditPageComponent implements OnInit {
  private readonly auditService = inject(TenantAuditService);

  readonly events = signal<TenantAuditEventResponse[]>([]);
  readonly page = signal<PageResponse<TenantAuditEventResponse> | null>(null);
  readonly isLoading = signal(false);
  readonly selectedEvent = signal<TenantAuditEventResponse | null>(null);

  // Filtros interactivos (Criterio 3)
  readonly searchFilter = signal('');
  readonly startDateFilter = signal('');
  readonly endDateFilter = signal('');
  readonly actorFilter = signal('');
  readonly moduleFilter = signal('all');
  readonly actionFilter = signal('all');
  readonly resourceTypeFilter = signal('all');

  readonly currentPage = computed(() => this.page()?.page ?? 0);
  readonly totalPages = computed(() => this.page()?.totalPages ?? 0);
  readonly totalElements = computed(() => this.page()?.totalElements ?? 0);

  readonly actorCount = computed(() => new Set(this.events().map(e => e.actorId || e.actorEmail || e.actorSubject)).size);
  readonly eventTypeCount = computed(() => new Set(this.events().map(e => e.eventType)).size);

  ngOnInit(): void {
    void this.reload(0);
  }

  async reload(page = this.currentPage()): Promise<void> {
    this.isLoading.set(true);

    const filters: TenantAuditFilters = {
      search: this.searchFilter() || null,
      startDate: this.startDateFilter() ? new Date(this.startDateFilter()).toISOString() : null,
      endDate: this.endDateFilter() ? new Date(this.endDateFilter() + 'T23:59:59.999Z').toISOString() : null,
      actor: this.actorFilter() || null,
      module: this.moduleFilter() !== 'all' ? this.moduleFilter() : null,
      action: this.actionFilter() !== 'all' ? this.actionFilter() : null,
      resourceType: this.resourceTypeFilter() !== 'all' ? this.resourceTypeFilter() : null
    };

    try {
      const response = await firstValueFrom(
        this.auditService.getAuditEvents(filters, page, 20).pipe(timeout(15000))
      );
      if (response.success && response.data) {
        this.events.set(response.data.content);
        this.page.set(response.data);
      }
    } catch {
      this.events.set([]);
      this.page.set(null);
    } finally {
      this.isLoading.set(false);
    }
  }

  applyFilters(): void {
    void this.reload(0);
  }

  resetFilters(): void {
    this.searchFilter.set('');
    this.startDateFilter.set('');
    this.endDateFilter.set('');
    this.actorFilter.set('');
    this.moduleFilter.set('all');
    this.actionFilter.set('all');
    this.resourceTypeFilter.set('all');
    void this.reload(0);
  }

  onPageChange(newPage: number): void {
    void this.reload(newPage);
  }

  openDetailModal(event: TenantAuditEventResponse): void {
    this.selectedEvent.set(event);
  }

  closeDetailModal(): void {
    this.selectedEvent.set(null);
  }

  formatDate(value: string | null | undefined): string {
    if (!value) return 'N/A';
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString('es-BO', { dateStyle: 'short', timeStyle: 'short' });
  }

  formatFullDate(value: string | null | undefined): string {
    if (!value) return 'N/A';
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString('es-BO', { dateStyle: 'full', timeStyle: 'medium' });
  }

  formatJson(value: string | null | undefined): string {
    if (!value) return 'N/A';
    try {
      const parsed = JSON.parse(value);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return value;
    }
  }

  getActorDisplayName(event: TenantAuditEventResponse): string {
    const subject = event?.actorSubject?.trim();
    const isUuid = !!subject && /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(subject);
    
    if (subject && !isUuid && subject.toUpperCase() !== 'SYSTEM') {
      return subject;
    }
    
    if (event?.actorEmail) {
      const emailUser = event.actorEmail.split('@')[0];
      return emailUser.replace(/[._]/g, ' ');
    }
    
    return 'Sistema';
  }

  getEventActionLabel(eventType: string): string {
    const code = (eventType || '').trim().toUpperCase();
    const map: Record<string, string> = {
      // Roles y Accesos
      'USER_ROLES_ASSIGNED': 'Asignación de roles',
      'USER_ROLES_UPDATED': 'Actualización de roles de usuario',
      'ROLE_CREATED': 'Creación de rol',
      'ROLE_UPDATED': 'Actualización de rol',
      'ROLE_ACTIVATED': 'Activación de rol',
      'ROLE_DEACTIVATED': 'Desactivación de rol',
      // Usuarios
      'USER_CREATED': 'Creación de usuario',
      'USER_UPDATED': 'Edición de usuario',
      'USER_ACTIVATED': 'Activación de usuario',
      'USER_DEACTIVATED': 'Desactivación de usuario',
      // Sesiones y Seguridad
      'TOKEN_REFRESHED': 'Renovación de sesión',
      'LOGIN_SUCCESS': 'Inicio de sesión exitoso',
      'LOGIN_FAILED': 'Inicio de sesión fallido',
      'LOGOUT': 'Cierre de sesión',
      'PASSWORD_CHANGED': 'Cambio de contraseña',
      'PASSWORD_RESET_REQUESTED': 'Solicitud de restablecimiento de contraseña',
      // Empleados y Expedientes
      'EMPLOYEE_CREATED': 'Registro de empleado',
      'EMPLOYEE_UPDATED': 'Actualización de empleado',
      'EMPLOYEE_ACTIVATED': 'Activación de empleado',
      'EMPLOYEE_DEACTIVATED': 'Desactivación de empleado',
      'EMPLOYEE_TERMINATED': 'Baja de empleado',
      'EMPLOYEE_REHIRED': 'Recontratación de empleado',
      'EMPLOYEE_TRANSFERRED': 'Transferencia de empleado',
      'EMPLOYEE_DOCUMENT_UPLOADED': 'Carga de documento de empleado',
      'EMPLOYEE_DOCUMENT_VERIFIED': 'Verificación de documento',
      'EMPLOYEE_DOCUMENT_ARCHIVED': 'Archivado de documento',
      // Contratos
      'CONTRACT_CREATED': 'Creación de contrato laboral',
      'CONTRACT_UPDATED': 'Actualización de contrato laboral',
      'CONTRACT_RENEWED': 'Renovación de contrato laboral',
      'CONTRACT_STATUS_UPDATED': 'Cambio de estado de contrato',
      // Departamentos y Puestos
      'DEPARTMENT_CREATED': 'Creación de departamento',
      'DEPARTMENT_UPDATED': 'Actualización de departamento',
      'DEPARTMENT_ACTIVATED': 'Activación de departamento',
      'DEPARTMENT_DEACTIVATED': 'Desactivación de departamento',
      'POSITION_CREATED': 'Creación de cargo',
      'POSITION_UPDATED': 'Actualización de cargo',
      'POSITION_ACTIVATED': 'Activación de cargo',
      'POSITION_DEACTIVATED': 'Desactivación de cargo',
      // Turnos y Asistencias
      'SHIFT_CREATED': 'Creación de turno laboral',
      'SHIFT_UPDATED': 'Actualización de turno laboral',
      'SHIFT_ACTIVATED': 'Activación de turno laboral',
      'SHIFT_DEACTIVATED': 'Desactivación de turno laboral',
      'ATTENDANCE_PUNCH_RECORDED': 'Marcación de asistencia',
      'ATTENDANCE_ADJUSTMENT_REQUESTED': 'Solicitud de ajuste de marcación',
      'ATTENDANCE_ADJUSTMENT_REVIEWED': 'Revisión de ajuste de asistencia',
      'ATTENDANCE_OVERTIME_REQUESTED': 'Solicitud de horas extras',
      'ATTENDANCE_OVERTIME_REVIEWED': 'Revisión de horas extras',
      // Ausencias
      'LEAVE_REQUEST_CREATED': 'Solicitud de ausencia/vacaciones',
      'LEAVE_REQUEST_APPROVED': 'Aprobación de ausencia',
      'LEAVE_REQUEST_REJECTED': 'Rechazo de ausencia',
      'LEAVE_REQUEST_CANCELLED': 'Cancelación de ausencia',
      // Respaldos y Configuración
      'BACKUP_CREATED': 'Creación de respaldo de seguridad',
      'BACKUP_RESTORED': 'Restauración de respaldo',
      'TENANT_SETTINGS_UPDATED': 'Actualización de configuración de empresa'
    };

    if (map[code]) return map[code];

    return code
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  getResourceLabel(resourceType: string): string {
    const code = (resourceType || '').trim().toUpperCase();
    const map: Record<string, string> = {
      'USER': 'Usuario',
      'ROLE': 'Rol',
      'EMPLOYEE': 'Empleado',
      'CONTRACT': 'Contrato',
      'EMPLOYEE_DOCUMENT': 'Documento de expediente',
      'SHIFT': 'Turno laboral',
      'ATTENDANCE_RECORD': 'Registro de asistencia',
      'ATTENDANCE_ADJUSTMENT': 'Ajuste de asistencia',
      'LEAVE_REQUEST': 'Solicitud de ausencia',
      'LEAVE_TYPE': 'Tipo de ausencia',
      'DEPARTMENT': 'Departamento',
      'POSITION': 'Cargo',
      'BACKUP': 'Respaldo',
      'TENANT_SETTINGS': 'Configuración de empresa'
    };
    return map[code] || code;
  }

  getOutcomeLabel(outcome: string | null | undefined): string {
    const code = (outcome || 'SUCCESS').trim().toUpperCase();
    const map: Record<string, string> = {
      'SUCCESS': 'Completado',
      'COMPLETED': 'Completado',
      'FAILURE': 'Fallido',
      'FAILED': 'Fallido',
      'ERROR': 'Error',
      'DENIED': 'Denegado',
      'PENDING': 'Pendiente'
    };
    return map[code] || code;
  }
}
