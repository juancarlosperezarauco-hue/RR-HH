import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { EmployeeFileService } from '../../entities/employee-file/api/employee-file.service';
import { TenantAuditEventResponse } from '../../entities/employee-file/model/document-audit.model';
import { PageResponse } from '../../shared/api';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';

@Component({
  selector: 'app-tenant-audit-page',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PlatformPaginationComponent],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Control del tenant
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">Auditoría</h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">Consulta las acciones administrativas y cambios relevantes de tu empresa.</p>
            </div>
          </div>
          <button type="button" (click)="reload()" [disabled]="isLoading()" class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50">
            <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon> Recargar
          </button>
        </div>
      </section>

      <section class="grid gap-4 sm:grid-cols-3">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm"><p class="text-sm font-semibold text-muted-foreground">Eventos en página</p><p class="mt-4 text-3xl font-black text-accent-foreground">{{ events().length }}</p><p class="mt-2 text-xs text-muted-foreground">Registros cargados</p></div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm"><p class="text-sm font-semibold text-muted-foreground">Actores</p><p class="mt-4 text-3xl font-black text-accent-foreground">{{ actorCount() }}</p><p class="mt-2 text-xs text-muted-foreground">Usuarios que realizaron acciones</p></div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm"><p class="text-sm font-semibold text-muted-foreground">Tipos de evento</p><p class="mt-4 text-3xl font-black text-accent-foreground">{{ eventTypeCount() }}</p><p class="mt-2 text-xs text-muted-foreground">Categorías observadas</p></div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 shadow-sm sm:p-6">
        <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"><div><h2 class="text-lg font-bold text-accent-foreground">Eventos registrados</h2><p class="text-sm text-muted-foreground">Ordenados del más reciente al más antiguo.</p></div><input type="search" [value]="query()" (input)="query.set($any($event.target).value)" placeholder="Buscar evento, actor o recurso" class="w-full rounded-2xl border border-border bg-muted px-4 py-3 text-sm text-accent-foreground outline-none focus:border-primary sm:max-w-xs" /></div>
        @if (isLoading()) { <div class="space-y-3">@for (item of [1,2,3,4]; track item) { <div class="h-16 animate-pulse rounded-2xl bg-muted"></div> }</div> } @else {
          <div class="overflow-x-auto"><table class="min-w-full divide-y divide-border"><thead class="bg-accent"><tr><th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-primary">Fecha</th><th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-primary">Actor</th><th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-primary">Evento</th><th class="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-primary">Recurso</th><th class="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-primary">Resultado</th></tr></thead><tbody class="divide-y divide-border bg-card">@for (event of filteredEvents(); track event.id) { <tr class="hover:bg-accent"><td class="whitespace-nowrap px-4 py-4 text-sm text-accent-foreground">{{ formatDate(event.createdAt) }}</td><td class="px-4 py-4"><div class="text-sm font-medium text-accent-foreground">{{ event.actorSubject || event.actorEmail || 'Usuario' }}</div><div class="text-xs text-muted-foreground">{{ event.actorEmail || 'Sin correo' }}</div></td><td class="px-4 py-4 text-sm font-medium text-accent-foreground">{{ event.eventType }}</td><td class="px-4 py-4"><div class="text-sm text-accent-foreground">{{ event.resourceType }}</div><div class="text-xs text-muted-foreground">{{ event.resourceId }}</div></td><td class="px-4 py-4 text-right"><span class="inline-flex rounded-full bg-accent px-2.5 py-1 text-xs font-semibold text-primary">{{ event.outcome || 'SUCCESS' }}</span></td></tr> } @empty { <tr><td colspan="5" class="px-4 py-10 text-center text-sm text-muted-foreground">No hay eventos de auditoría.</td></tr> }</tbody></table></div>
        }
        @if (page(); as currentPage) { <div class="mt-4"><app-platform-pagination [currentPage]="currentPage.page" [totalPages]="currentPage.totalPages" [totalElements]="currentPage.totalElements" [isLoading]="isLoading()" (pageChange)="reload($event)"></app-platform-pagination></div> }
      </section>
    </div>
  `
})
export class TenantAuditPageComponent implements OnInit {
  private readonly service = inject(EmployeeFileService);
  readonly events = signal<TenantAuditEventResponse[]>([]);
  readonly page = signal<PageResponse<TenantAuditEventResponse> | null>(null);
  readonly query = signal('');
  readonly isLoading = signal(false);
  readonly filteredEvents = computed(() => { const q = this.query().trim().toLowerCase(); return this.events().filter(event => !q || [event.actorSubject, event.actorEmail, event.eventType, event.resourceType, event.resourceId, event.eventDetails].join(' ').toLowerCase().includes(q)); });
  readonly actorCount = computed(() => new Set(this.events().map(event => event.actorId || event.actorEmail)).size);
  readonly eventTypeCount = computed(() => new Set(this.events().map(event => event.eventType)).size);

  ngOnInit(): void { void this.reload(); }

  async reload(page = this.page()?.page ?? 0): Promise<void> {
    this.isLoading.set(true);
    try {
      const response = await firstValueFrom(this.service.listAuditEvents(page, 20).pipe(timeout(15000)));
      if (response.success && response.data) { this.events.set(response.data.content); this.page.set(response.data); }
    } finally { this.isLoading.set(false); }
  }

  formatDate(value: string | null | undefined): string { if (!value) return 'N/A'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString('es-BO', { dateStyle: 'medium', timeStyle: 'short' }); }
}
