import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';
import { NotificationsService, NotificationResponse, PageResponse } from '../../entities/notifications';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [CommonModule, PlatformPaginationComponent, LucideAngularModule],
  providers: [DatePipe],
  template: `
    <div class="space-y-6 relative">
      <section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-border bg-card/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              <span class="h-2 w-2 rounded-full bg-secondary"></span>
              Bandeja de entrada
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Notificaciones
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
                Revisa, marca y archiva las notificaciones del tenant sin salir del panel.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="refreshAll()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent">
              <lucide-icon name="refresh-ccw" [size]="16"></lucide-icon>
              Recargar
            </button>
            <button
              type="button"
              (click)="markAllAsRead()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground shadow-sm transition-colors hover:bg-primary/90">
              <lucide-icon name="check-circle" [size]="16"></lucide-icon>
              Marcar todo como leído
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">No leídas</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ unreadCount() }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Pendientes por revisar</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">En página</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ currentPageCount() }}</p>
          <p class="mt-2 text-xs text-muted-foreground">Resultados visibles</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Leídas</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ readVisibleCount() }}</p>
          <p class="mt-2 text-xs text-muted-foreground">En esta vista</p>
        </div>
        <div class="rounded-2xl border border-border bg-card p-5 shadow-sm">
          <p class="text-sm font-semibold text-muted-foreground">Archivadas</p>
          <p class="mt-4 text-3xl font-black text-accent-foreground">{{ archivedVisibleCount() }}</p>
          <p class="mt-2 text-xs text-muted-foreground">En esta vista</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm space-y-4">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-lg font-bold text-accent-foreground">Filtros</h2>
            <p class="text-sm text-muted-foreground">Filtra por estado o busca por texto libre.</p>
          </div>
          <label class="relative w-full lg:max-w-md">
            <lucide-icon name="search" class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" [size]="16"></lucide-icon>
            <input
              type="text"
              [value]="searchTerm()"
              (input)="onSearch(($any($event.target).value || '').toString())"
              placeholder="Buscar notificación..."
              class="w-full rounded-full border border-border bg-muted py-3 pl-10 pr-4 text-sm text-accent-foreground outline-none transition focus:border-primary focus:bg-card">
          </label>
        </div>

        <div class="flex flex-wrap gap-2">
          @for (item of filters; track item.value) {
            <button
              type="button"
              (click)="filter.set(item.value)"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border px-4 py-2 text-sm font-semibold transition-colors"
              [ngClass]="filter() === item.value ? 'border-primary bg-primary text-primary-foreground shadow-sm' : 'border-border bg-card text-primary hover:bg-accent'">
              {{ item.label }}
            </button>
          }
        </div>
      </section>

      @if (status() === 'error') {
        <div class="rounded-2xl border border-destructive/20 bg-destructive/5 p-6 flex items-start gap-4">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-destructive mt-0.5"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
          <div>
            <h3 class="font-semibold text-destructive">Error al cargar notificaciones</h3>
            <p class="mt-1 text-sm text-destructive/80">{{ error() }}</p>
            <button (click)="refreshAll()" class="mt-3 cursor-pointer text-sm font-semibold text-destructive hover:underline">Intentar nuevamente</button>
          </div>
        </div>
      }

      @if (status() === 'loading' && visibleNotifications().length === 0) {
        <div class="flex flex-col items-center justify-center gap-4 p-12 text-muted-foreground">
          <svg class="h-8 w-8 animate-spin text-primary" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p>Cargando notificaciones...</p>
        </div>
      }

      @if (status() === 'success' || (status() === 'loading' && visibleNotifications().length > 0)) {
        <div class="space-y-3">
          @for (notification of visibleNotifications(); track notification.id) {
            <article class="rounded-[24px] border border-border bg-card p-4 sm:p-5 shadow-sm transition-colors hover:bg-background">
              <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div class="min-w-0 space-y-2">
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="rounded-full px-2.5 py-1 text-xs font-semibold" [ngClass]="notification.status === 'UNREAD' ? 'bg-primary/10 text-primary' : 'bg-muted-foreground/10 text-muted-foreground'">
                      {{ statusLabel(notification.status) }}
                    </span>
                    <span class="rounded-full border border-border bg-card px-2.5 py-1 text-xs font-semibold text-muted-foreground">
                      {{ priorityLabel(notification.priority) }}
                    </span>
                    <span class="rounded-full border border-border bg-card px-2.5 py-1 text-xs font-semibold text-muted-foreground">
                      {{ notification.category }}
                    </span>
                  </div>
                  <h3 class="text-base font-bold text-accent-foreground break-words">{{ notification.title }}</h3>
                  <p class="text-sm leading-6 text-muted-foreground break-words">{{ notification.body }}</p>
                  <div class="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                    <span>{{ notification.type }}</span>
                    <span>•</span>
                    <span>{{ notification.createdAt | date:'short' }}</span>
                    @if (notification.actionUrl) {
                      <span>•</span>
                      <a class="font-semibold text-primary hover:underline" [href]="notification.actionUrl" target="_blank" rel="noreferrer">Acción</a>
                    }
                  </div>
                </div>

                <div class="flex flex-wrap gap-2 lg:justify-end">
                  <button
                    type="button"
                    (click)="openDetail(notification)"
                    class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border px-3 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-accent">
                    <lucide-icon name="eye" [size]="14"></lucide-icon>
                    Ver
                  </button>
                  <button
                    type="button"
                    (click)="markAsRead(notification)"
                    class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border px-3 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-accent">
                    <lucide-icon name="check-circle" [size]="14"></lucide-icon>
                    Leída
                  </button>
                  <button
                    type="button"
                    (click)="archive(notification)"
                    class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border px-3 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-accent">
                    <lucide-icon name="archive" [size]="14"></lucide-icon>
                    Archivar
                  </button>
                </div>
              </div>
            </article>
          } @empty {
            <div class="rounded-2xl border border-dashed border-border bg-background p-8 text-center text-sm text-muted-foreground">
              No hay notificaciones para el filtro seleccionado.
            </div>
          }
        </div>

        <div class="rounded-[24px] border border-border bg-card p-4 sm:p-6 shadow-sm">
          <app-platform-pagination
            [currentPage]="currentPage()"
            [totalPages]="totalPages()"
            [totalElements]="totalElements()"
            [isLoading]="status() === 'loading'"
            (pageChange)="onPageChange($event)">
          </app-platform-pagination>
        </div>
      }

      @if (selectedNotification()) {
        <div class="app-modal-overlay">
          <div class="app-modal-panel app-modal-panel-sm">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Detalle de notificación</h2>
                <p class="app-modal-subtitle">Información operativa y metadatos del evento.</p>
              </div>
              <button
                type="button"
                class="inline-flex cursor-pointer items-center justify-center rounded-full border border-border bg-card p-2 text-primary transition-colors hover:bg-accent"
                (click)="closeDetail()">
                <lucide-icon name="x" [size]="18"></lucide-icon>
              </button>
            </div>

            <div class="app-modal-content-grid">
              <div class="rounded-2xl border border-border bg-background p-4">
                <p class="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Título</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground break-words">{{ selectedNotification()?.title }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-background p-4">
                <p class="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Estado</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ statusLabel(selectedNotification()?.status || '') }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-background p-4">
                <p class="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Categoría</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ selectedNotification()?.category }}</p>
              </div>
              <div class="rounded-2xl border border-border bg-background p-4">
                <p class="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Prioridad</p>
                <p class="mt-1 text-sm font-semibold text-accent-foreground">{{ priorityLabel(selectedNotification()?.priority || '') }}</p>
              </div>
            </div>

            <div class="rounded-2xl border border-border bg-card p-4 space-y-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Mensaje</p>
                <p class="mt-2 text-sm leading-6 text-muted-foreground break-words">{{ selectedNotification()?.body }}</p>
              </div>

              @if (selectedNotification()?.actionUrl) {
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Acción</p>
                  <a class="mt-2 inline-flex cursor-pointer items-center gap-2 text-sm font-semibold text-primary hover:underline" [href]="selectedNotification()?.actionUrl" target="_blank" rel="noreferrer">
                    Abrir enlace
                  </a>
                </div>
              }

              @if (dataEntries().length > 0) {
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.12em] text-muted-foreground">Datos</p>
                  <div class="mt-3 grid gap-3 sm:grid-cols-2">
                    @for (entry of dataEntries(); track entry[0]) {
                      <div class="rounded-2xl border border-border bg-background p-3">
                        <p class="text-[11px] font-semibold uppercase tracking-[0.12em] text-muted-foreground">{{ entry[0] }}</p>
                        <p class="mt-1 text-sm font-semibold text-accent-foreground break-words">{{ entry[1] }}</p>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>

            <div class="app-modal-footer">
              <button
                type="button"
                class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-accent"
                (click)="closeDetail()">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class NotificationsPageComponent implements OnInit {
  private readonly notificationsService = inject(NotificationsService);

  readonly pageSize = 20;
  readonly status = signal<'idle' | 'loading' | 'success' | 'error'>('idle');
  readonly error = signal('');
  readonly page = signal<PageResponse<NotificationResponse> | null>(null);
  readonly selectedNotification = signal<NotificationResponse | null>(null);
  readonly unreadCount = signal(0);
  readonly filter = signal<'all' | 'unread' | 'read' | 'archived'>('all');
  readonly searchTerm = signal('');

  readonly filters = [
    { label: 'Todas', value: 'all' as const },
    { label: 'No leídas', value: 'unread' as const },
    { label: 'Leídas', value: 'read' as const },
    { label: 'Archivadas', value: 'archived' as const }
  ];

  readonly currentPage = computed(() => this.page()?.number ?? 0);
  readonly totalPages = computed(() => this.page()?.totalPages ?? 0);
  readonly totalElements = computed(() => this.page()?.totalElements ?? 0);
  readonly visibleNotifications = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    return (this.page()?.content ?? []).filter((notification) => {
      const matchesFilter = this.filter() === 'all'
        ? true
        : this.filter() === 'unread'
          ? notification.status === 'UNREAD'
          : this.filter() === 'read'
            ? notification.status === 'READ'
            : notification.status === 'ARCHIVED';

      const matchesSearch = !term || [
        notification.title,
        notification.body,
        notification.type,
        notification.category,
        notification.priority,
        notification.status
      ].some((value) => String(value ?? '').toLowerCase().includes(term));

      return matchesFilter && matchesSearch;
    });
  });
  readonly currentPageCount = computed(() => this.visibleNotifications().length);
  readonly readVisibleCount = computed(() => this.visibleNotifications().filter((item) => item.status === 'READ').length);
  readonly archivedVisibleCount = computed(() => this.visibleNotifications().filter((item) => item.status === 'ARCHIVED').length);
  readonly dataEntries = computed(() => {
    const data = this.selectedNotification()?.data;
    if (!data || typeof data !== 'object' || Array.isArray(data)) {
      return [] as Array<[string, string]>;
    }

    return Object.entries(data)
      .slice(0, 6)
      .map(([key, value]) => [key, typeof value === 'object' ? JSON.stringify(value) : String(value)] as [string, string]);
  });

  ngOnInit(): void {
    this.refreshAll();
  }

  async refreshAll(): Promise<void> {
    await Promise.all([
      this.loadUnreadCount(),
      this.loadNotifications(this.currentPage())
    ]);
  }

  async loadNotifications(page = 0): Promise<void> {
    this.status.set('loading');
    this.error.set('');

    try {
      const response = await firstValueFrom(this.notificationsService.listNotifications(page, this.pageSize));
      if (response.success && response.data) {
        this.page.set(response.data);
        this.status.set('success');
      } else {
        this.page.set(null);
        this.status.set('error');
        this.error.set(response.message || 'No se pudieron cargar las notificaciones');
      }
    } catch (err: any) {
      this.page.set(null);
      this.status.set('error');
      this.error.set(err.error?.message || err.message || 'Error al conectar con el servidor');
    }
  }

  async loadUnreadCount(): Promise<void> {
    try {
      const response = await firstValueFrom(this.notificationsService.unreadCount());
      if (response.success && response.data) {
        this.unreadCount.set(response.data.unreadCount);
      }
    } catch {
      this.unreadCount.set(0);
    }
  }

  async onPageChange(page: number): Promise<void> {
    await this.loadNotifications(page);
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
  }

  async markAllAsRead(): Promise<void> {
    try {
      await firstValueFrom(this.notificationsService.markAllAsRead());
      await this.refreshAll();
    } catch {
      // el handler global mostrará el error
    }
  }

  async markAsRead(notification: NotificationResponse): Promise<void> {
    try {
      await firstValueFrom(this.notificationsService.markNotificationAsRead(notification.id));
      await this.refreshAll();
    } catch {
      // el handler global mostrará el error
    }
  }

  async archive(notification: NotificationResponse): Promise<void> {
    try {
      await firstValueFrom(this.notificationsService.archiveNotification(notification.id));
      await this.refreshAll();
    } catch {
      // el handler global mostrará el error
    }
  }

  async openDetail(notification: NotificationResponse): Promise<void> {
    try {
      const response = await firstValueFrom(this.notificationsService.getNotificationById(notification.id));
      if (response.success && response.data) {
        this.selectedNotification.set(response.data);
        await firstValueFrom(this.notificationsService.openNotification(notification.id));
        await this.refreshAll();
        return;
      }
    } catch {
      // fallback below
    }

    this.selectedNotification.set(notification);
    if (notification.status === 'UNREAD') {
      await this.markAsRead(notification);
    }
  }

  closeDetail(): void {
    this.selectedNotification.set(null);
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      UNREAD: 'No leída',
      READ: 'Leída',
      ARCHIVED: 'Archivada',
      EXPIRED: 'Expirada'
    };

    return labels[status] ?? status;
  }

  priorityLabel(priority: string): string {
    const labels: Record<string, string> = {
      LOW: 'Baja',
      NORMAL: 'Normal',
      HIGH: 'Alta',
      CRITICAL: 'Crítica'
    };

    return labels[priority] ?? priority;
  }
}
