import { Component, EventEmitter, Output, HostListener, ElementRef, effect, inject, OnDestroy, OnInit, signal, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { trigger, transition, style, animate } from '@angular/animations';
import { firstValueFrom } from 'rxjs';
import { AuthFacade } from '../../../shared/lib/auth/auth.facade';
import { PlatformFacade } from '../../../features/platform/lib/platform.facade';
import { PermissionService } from '../../../shared/lib/auth/permission.service';
import { NotificationsService, NotificationResponse } from '../../../entities/notifications';
import { AuthService } from '../../../entities/auth/api/auth.service';
import { NavigationScope } from '../../../shared/lib/navigation';
import { TenantLanguageStateService } from '../../../shared/lib/i18n/tenant-language-state.service';
import { TenantTextKey } from '../../../shared/lib/i18n/keys';

interface MenuItem {
  label: string;
  description?: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  animations: [
    trigger('dropdownAnimation', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.95) translateY(-8px)' }),
        animate('200ms cubic-bezier(0.16, 1, 0.3, 1)', style({ opacity: 1, transform: 'scale(1) translateY(0)' }))
      ]),
      transition(':leave', [
        animate('150ms cubic-bezier(0.4, 0, 0.2, 1)', style({ opacity: 0, transform: 'scale(0.96) translateY(-6px)' }))
      ])
    ])
  ],
  template: `
    <header class="hrcore-brand-theme sticky top-0 z-30 flex h-16 w-full items-center justify-between gap-4 border-b border-border bg-card/95 px-6 shadow-xs backdrop-blur-md md:px-8">
      <!-- Lado izquierdo: Título principal + Módulo actual -->
      <div class="flex min-w-0 items-center gap-3">
        <div class="ml-10 flex items-center gap-2.5 md:ml-0">
          <span class="hidden h-2.5 w-2.5 rounded-full bg-primary shadow-[0_0_0_4px_rgba(8,67,147,0.12)] sm:inline-block"></span>
          <h1 class="truncate text-lg font-black tracking-tight text-primary md:text-xl">
            @if (isPlatformRoute) {
              Panel de Control · Plataforma
            } @else {
              {{ t('shell.dashboard') }}
            }
          </h1>
        </div>

        <div class="hidden h-5 w-px bg-border sm:block"></div>

        <!-- Breadcrumb / Indicador de vista activa -->
        <div class="hidden items-center gap-2 rounded-xl border border-border/80 bg-accent/55 px-3 py-1.5 text-xs font-bold text-primary shadow-2xs sm:inline-flex">
          <lucide-icon [name]="getCurrentSectionIcon()" class="h-3.5 w-3.5 shrink-0 text-primary"></lucide-icon>
          <span class="truncate max-w-[200px]">{{ getCurrentSectionLabel() }}</span>
        </div>
      </div>

      <!-- Centro / Derecha formal minimalista empresarial -->
      <div class="relative flex items-center gap-2 md:gap-3">
        <!-- Píldoras informativas empresariales (Desktop) -->
        <div class="hidden xl:flex items-center gap-2.5 mr-1">
          <div class="inline-flex items-center gap-2 rounded-xl border border-border/80 bg-background/80 px-3 py-1.5 text-xs font-semibold text-muted-foreground shadow-2xs">
            <span class="relative flex h-2 w-2">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span class="text-foreground font-bold">{{ isPlatformRoute ? 'Nodo Global Activo' : 'Entorno Corporativo' }}</span>
          </div>

          <div class="inline-flex items-center gap-2 rounded-xl border border-border/80 bg-background/80 px-3 py-1.5 text-xs font-semibold text-muted-foreground shadow-2xs">
            <lucide-icon name="calendar" class="h-3.5 w-3.5 text-primary"></lucide-icon>
            <span class="capitalize text-foreground font-semibold">{{ getFormattedToday() }}</span>
          </div>
        </div>
        @if (!isPlatformRoute) {
          <div class="relative">
            <button
              type="button"
              (click)="toggleNotificationsMenu()"
              class="relative flex h-10 w-10 cursor-pointer items-center justify-center rounded-xl border border-transparent text-primary transition-all duration-200 hover:border-border hover:bg-accent"
              [class.bg-accent]="isNotificationsOpen"
              [class.border-border]="isNotificationsOpen"
              aria-label="Notificaciones">
              <lucide-icon name="bell" class="h-5 w-5"></lucide-icon>
              @if (unreadNotifications() > 0) {
                <span class="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-bold text-primary-foreground shadow-xs">
                  {{ unreadNotifications() }}
                </span>
              }
            </button>

            @if (isNotificationsOpen) {
              <div
                @dropdownAnimation
                class="absolute right-0 z-50 mt-2.5 w-96 max-w-[calc(100vw-1.5rem)] overflow-hidden rounded-[24px] border border-border bg-card shadow-2xl">
                <div class="flex items-center justify-between border-b border-border bg-accent/70 px-4 py-3.5">
                  <div>
                    <p class="text-sm font-black text-primary">{{ t('shell.notifications') }}</p>
                    <p class="text-xs text-muted-foreground">{{ unreadNotifications() }} {{ t('shell.unread') }}</p>
                  </div>
                  <button
                    type="button"
                    (click)="markAllAsRead()"
                    class="cursor-pointer rounded-full border border-border bg-card px-3 py-1 text-xs font-bold text-primary transition-colors hover:bg-accent">
                    {{ t('shell.markAll') }}
                  </button>
                </div>

                <div class="max-h-96 overflow-y-auto">
                  @if (notificationsLoading) {
                    <div class="flex items-center justify-center gap-3 p-6 text-sm text-muted-foreground">
                      <lucide-icon name="loader-2" [size]="16" class="animate-spin"></lucide-icon>
                      {{ t('shell.loading') }}
                    </div>
                  } @else {
                    @for (notification of notificationPreview(); track notification.id) {
                      <button
                        type="button"
                        (click)="openNotification(notification)"
                        class="flex w-full cursor-pointer gap-3 border-b border-border px-4 py-3 text-left transition-colors hover:bg-accent/40">
                        <div class="mt-1 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                          <lucide-icon name="bell" [size]="16"></lucide-icon>
                        </div>
                        <div class="min-w-0 flex-1">
                          <div class="flex items-center justify-between gap-2">
                            <p class="truncate text-sm font-bold text-accent-foreground">{{ notification.title }}</p>
                            @if (notification.status === 'UNREAD') {
                              <span class="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">{{ t('shell.new') }}</span>
                            }
                          </div>
                          <p class="mt-1 line-clamp-2 text-xs leading-5 text-muted-foreground">{{ notification.body }}</p>
                          <p class="mt-1 text-[11px] text-muted-foreground">{{ notification.createdAt | date:'short' }}</p>
                        </div>
                      </button>
                    } @empty {
                      <div class="p-6 text-center text-sm text-muted-foreground">
                        {{ t('shell.noNotifications') }}
                      </div>
                    }
                  }
                </div>

                <div class="border-t border-border p-3">
                  <button
                    type="button"
                    (click)="goToNotifications()"
                    class="inline-flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-primary-foreground shadow-xs transition-all hover:bg-primary/90">
                    {{ t('shell.inbox') }}
                  </button>
                </div>
              </div>
            }
          </div>
        }

        <div class="mx-1 h-6 w-px bg-border"></div>

        <!-- Botón de Perfil y Menú Desplegable Mejorado -->
        <div class="relative">
          <button
            type="button"
            (click)="toggleProfileMenu()"
            [attr.aria-expanded]="isProfileOpen"
            class="group flex cursor-pointer items-center gap-2.5 rounded-2xl border border-transparent px-2.5 py-1.5 transition-all duration-200 hover:border-border hover:bg-accent/70 focus:outline-none md:gap-3"
            [class.bg-accent]="isProfileOpen"
            [class.border-border]="isProfileOpen"
            [class.shadow-xs]="isProfileOpen"
          >
            <div class="relative flex h-9 w-9 shrink-0 overflow-hidden rounded-xl border border-primary/25 bg-primary/10 text-sm font-black uppercase text-primary shadow-2xs transition-transform duration-200 group-hover:scale-105">
              @if (getProfilePhotoUrl()) {
                <img
                  [src]="getProfilePhotoUrl()"
                  [alt]="getDisplayName() || (isPlatformRoute ? 'Usuario' : t('shell.user'))"
                  class="h-full w-full object-cover">
              } @else {
                <div class="flex h-full w-full items-center justify-center">
                  {{ getInitials() }}
                </div>
              }
              <span class="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-card bg-emerald-500"></span>
            </div>
            <div class="hidden flex-col items-start text-left md:flex">
              <span class="max-w-[160px] truncate text-sm font-bold leading-tight text-accent-foreground">
                {{ getDisplayName() }}
              </span>
              <span class="mt-0.5 max-w-[160px] truncate text-[11px] font-semibold text-muted-foreground">
                {{ getRole() }}
              </span>
            </div>
            <lucide-icon
              name="chevron-down"
              class="hidden h-4 w-4 text-muted-foreground transition-transform duration-300 ease-out md:block"
              [class.rotate-180]="isProfileOpen"
              [class.text-primary]="isProfileOpen"
            ></lucide-icon>
          </button>

          <div
            *ngIf="isProfileOpen"
            @dropdownAnimation
            class="absolute right-0 z-50 mt-2.5 w-80 max-w-[calc(100vw-1.5rem)] overflow-hidden rounded-[24px] border border-border bg-card p-2 shadow-2xl ring-1 ring-sidebar/5 focus:outline-none"
          >
            <!-- Cabecera del perfil ampliada -->
            <div class="rounded-2xl border border-border/70 bg-gradient-to-br from-accent/80 via-card to-accent/40 p-4">
              <div class="flex items-center gap-3.5">
                <div class="flex h-12 w-12 shrink-0 overflow-hidden rounded-2xl border border-primary/25 bg-primary text-base font-black uppercase text-primary-foreground shadow-sm">
                  @if (getProfilePhotoUrl()) {
                    <img
                      [src]="getProfilePhotoUrl()"
                      [alt]="getDisplayName() || 'Usuario'"
                      class="h-full w-full object-cover">
                  } @else {
                    <div class="flex h-full w-full items-center justify-center">
                      {{ getInitials() }}
                    </div>
                  }
                </div>
                <div class="min-w-0 flex-1">
                  <p class="truncate text-base font-black text-accent-foreground">
                    {{ getDisplayName() }}
                  </p>
                  <p class="truncate text-xs font-medium text-muted-foreground">
                    {{ getEmail() }}
                  </p>
                  <span class="mt-1.5 inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-[11px] font-bold text-primary">
                    <lucide-icon name="shield-check" class="h-3 w-3"></lucide-icon>
                    <span class="truncate">{{ getRole() }}</span>
                  </span>
                </div>
              </div>
            </div>

            <!-- Opciones de navegación con iconos y descripción -->
            <div class="mt-1.5 space-y-1 py-1">
              @if (isPlatformRoute) {
                @for (item of platformMenuOptions; track item.route) {
                  <a
                    [routerLink]="item.route"
                    (click)="isProfileOpen = false"
                    class="group flex cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 transition-all duration-200 hover:translate-x-0.5 hover:bg-accent">
                    <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-muted text-primary transition-colors duration-200 group-hover:bg-primary group-hover:text-primary-foreground">
                      <lucide-icon [name]="item.icon" class="h-4 w-4"></lucide-icon>
                    </div>
                    <div class="min-w-0 flex-1">
                      <p class="text-sm font-bold text-foreground transition-colors group-hover:text-primary">{{ item.label }}</p>
                      @if (item.description) {
                        <p class="truncate text-[11px] text-muted-foreground">{{ item.description }}</p>
                      }
                    </div>
                    <lucide-icon name="arrow-right" class="h-3.5 w-3.5 text-muted-foreground opacity-0 transition-all duration-200 group-hover:translate-x-0.5 group-hover:text-primary group-hover:opacity-100"></lucide-icon>
                  </a>
                }
              } @else {
                @for (item of tenantMenuOptions; track item.route) {
                  <a
                    [routerLink]="item.route"
                    (click)="isProfileOpen = false"
                    class="group flex cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 transition-all duration-200 hover:translate-x-0.5 hover:bg-accent">
                    <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-muted text-primary transition-colors duration-200 group-hover:bg-primary group-hover:text-primary-foreground">
                      <lucide-icon [name]="item.icon" class="h-4 w-4"></lucide-icon>
                    </div>
                    <div class="min-w-0 flex-1">
                      <p class="text-sm font-bold text-foreground transition-colors group-hover:text-primary">{{ item.label }}</p>
                      @if (item.description) {
                        <p class="truncate text-[11px] text-muted-foreground">{{ item.description }}</p>
                      }
                    </div>
                    <lucide-icon name="arrow-right" class="h-3.5 w-3.5 text-muted-foreground opacity-0 transition-all duration-200 group-hover:translate-x-0.5 group-hover:text-primary group-hover:opacity-100"></lucide-icon>
                  </a>
                }
              }
            </div>

            <!-- Botón de cierre de sesión -->
            <div class="mt-1 border-t border-border pt-1.5">
              <button
                type="button"
                (click)="onLogout()"
                class="group flex w-full cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-all duration-200 hover:bg-destructive/10"
              >
                <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-destructive/10 text-destructive transition-colors duration-200 group-hover:bg-destructive group-hover:text-destructive-foreground">
                  <lucide-icon name="log-out" class="h-4 w-4"></lucide-icon>
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm font-bold text-destructive">{{ isPlatformRoute ? 'Cerrar Sesión' : t('shell.logout') }}</p>
                  <p class="text-[11px] text-muted-foreground">Salir de tu cuenta de forma segura</p>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  `
})
export class HeaderComponent implements OnInit, OnDestroy {
  @Input({ required: true }) scope!: NavigationScope;
  @Output() logoutAction = new EventEmitter<void>();
  isProfileOpen = false;
  isNotificationsOpen = false;
  notificationsLoading = false;
  readonly notificationPreview = signal<NotificationResponse[]>([]);
  readonly unreadNotifications = signal(0);

  readonly platformMenuOptions: MenuItem[] = [
    { label: 'Mi perfil', description: 'Datos de cuenta SuperAdmin', route: '/platform/profile', icon: 'user-circle-2' },
    { label: 'Seguridad', description: 'Contraseña y sesiones activas', route: '/platform/security', icon: 'lock' },
    { label: 'Configuraciones', description: 'Parámetros globales del sistema', route: '/platform/settings', icon: 'settings' },
    { label: 'Respaldos', description: 'Copias de seguridad de plataforma', route: '/platform/backups', icon: 'arrow-down-to-line' }
  ];

  private elementRef = inject(ElementRef);
  private router = inject(Router);
  private authFacade = inject(AuthFacade);
  private platformFacade = inject(PlatformFacade);
  private readonly permissionService = inject(PermissionService);
  private readonly notificationsService = inject(NotificationsService);
  private readonly languageState = inject(TenantLanguageStateService);
  private readonly authService = inject(AuthService);
  private readonly profilePhotoSource = signal<string | null>(null);
  private profilePhotoObjectUrl: string | null = null;
  private profilePhotoRequestKey: string | null = null;

  readonly platformSuperadmin = this.platformFacade.currentSuperadmin;
  readonly tenantUser = this.authFacade.currentUser;

  constructor() {
    effect(() => {
      const user = this.tenantUser();
      const photoUrl = user?.profilePhotoUrl ?? null;
      const requestKey = photoUrl ? `${photoUrl}|${user?.updatedAt ?? ''}` : null;
      if (requestKey === this.profilePhotoRequestKey) {
        return;
      }

      this.profilePhotoRequestKey = requestKey;
      if (!photoUrl || !requestKey) {
        this.clearProfilePhotoSource();
        return;
      }

      void this.loadProfilePhoto(photoUrl, requestKey);
    });
  }

  get tenantMenuOptions(): MenuItem[] {
    return [
      { label: this.t('shell.dashboard'), description: 'Estadísticas y resumen operativo', route: this.dashboardRoute, icon: 'layout-dashboard' },
      { label: this.t('nav.profile'), description: 'Información personal y biometría', route: '/dashboard/profile', icon: 'user-circle-2' },
      { label: 'Mi espacio laboral', description: 'Contrato, turnos y asistencias', route: '/dashboard/my-employment', icon: 'briefcase' },
      { label: this.t('shell.notifications'), description: 'Avisos y alertas de tu cuenta', route: '/dashboard/notifications', icon: 'bell' },
      { label: 'Seguridad y sesiones', description: 'Control de accesos y contraseña', route: '/dashboard/security', icon: 'shield-check' }
    ];
  }

  ngOnInit(): void {
    if (this.isPlatformRoute) {
      this.platformFacade.loadCurrentSuperadmin();
      return;
    }

    this.loadNotificationPreview();
  }

  get isPlatformRoute(): boolean {
    return this.scope === 'PLATFORM';
  }

  t(key: TenantTextKey): string { return this.languageState.t(key); }

  get isOwnerAdmin(): boolean {
    return this.permissionService.isCurrentOwner();
  }

  get dashboardRoute(): string {
    return '/dashboard/home';
  }

  getFormattedToday(): string {
    try {
      return new Intl.DateTimeFormat('es-BO', {
        weekday: 'short',
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }).format(new Date());
    } catch {
      return '';
    }
  }

  getCurrentSectionLabel(): string {
    const url = this.router.url.split('?')[0];
    const map: { prefix: string; label: string }[] = [
      { prefix: '/dashboard/home', label: 'Resumen Ejecutivo' },
      { prefix: '/dashboard/users', label: 'Gestión de Usuarios' },
      { prefix: '/dashboard/departments', label: 'Departamentos' },
      { prefix: '/dashboard/positions', label: 'Puestos de Trabajo' },
      { prefix: '/dashboard/organization-chart', label: 'Organigrama' },
      { prefix: '/dashboard/my-team', label: 'Mi Equipo' },
      { prefix: '/dashboard/employees', label: 'Colaboradores' },
      { prefix: '/dashboard/contracts', label: 'Contratos Laborales' },
      { prefix: '/dashboard/dossiers', label: 'Expedientes Digitales' },
      { prefix: '/dashboard/shifts', label: 'Horarios y Turnos' },
      { prefix: '/dashboard/attendance', label: 'Control de Asistencia' },
      { prefix: '/dashboard/absences', label: 'Ausencias y Permisos' },
      { prefix: '/dashboard/incidents', label: 'Incidentes y Alertas' },
      { prefix: '/dashboard/profile', label: 'Mi Perfil Personal' },
      { prefix: '/dashboard/my-employment', label: 'Mi Espacio Laboral' },
      { prefix: '/dashboard/notifications', label: 'Centro de Notificaciones' },
      { prefix: '/dashboard/roles', label: 'Roles y Permisos' },
      { prefix: '/dashboard/permissions', label: 'Catálogo de Permisos' },
      { prefix: '/dashboard/subscription', label: 'Suscripción y Plan' },
      { prefix: '/dashboard/hr-reports', label: 'Reportes de RR. HH.' },
      { prefix: '/dashboard/company-settings', label: 'Configuración de Empresa' },
      { prefix: '/dashboard/audit', label: 'Bitácora de Auditoría' },
      { prefix: '/dashboard/backups', label: 'Copias de Seguridad' },
      { prefix: '/dashboard/security', label: 'Seguridad y Sesiones' },
      { prefix: '/platform/dashboard', label: 'Métricas de Plataforma' },
      { prefix: '/platform/tenants', label: 'Empresas Suscritas' },
      { prefix: '/platform/plans', label: 'Planes Comerciales' },
      { prefix: '/platform/subscriptions', label: 'Suscripciones' },
      { prefix: '/platform/audit', label: 'Auditoría Global' },
      { prefix: '/platform/backups', label: 'Respaldos Globales' },
      { prefix: '/platform/profile', label: 'Perfil SuperAdmin' },
      { prefix: '/platform/security', label: 'Seguridad Global' },
      { prefix: '/platform/settings', label: 'Parámetros del Sistema' }
    ];
    const match = map.find(item => url.startsWith(item.prefix));
    return match ? match.label : (this.isPlatformRoute ? 'Consola Global' : 'Gestión RR. HH.');
  }

  getCurrentSectionIcon(): string {
    const url = this.router.url.split('?')[0];
    if (url.includes('/users') || url.includes('/employees') || url.includes('/my-team')) return 'users';
    if (url.includes('/departments') || url.includes('/tenants') || url.includes('/company-settings')) return 'building-2';
    if (url.includes('/positions') || url.includes('/contracts') || url.includes('/my-employment')) return 'briefcase';
    if (url.includes('/shifts') || url.includes('/attendance') || url.includes('/absences')) return 'calendar-clock';
    if (url.includes('/roles') || url.includes('/permissions') || url.includes('/security') || url.includes('/audit')) return 'shield-check';
    if (url.includes('/dossiers') || url.includes('/hr-reports')) return 'file-text';
    if (url.includes('/notifications')) return 'bell';
    return 'layout-dashboard';
  }

  getDisplayName(): string {
    if (this.isPlatformRoute) {
      const user = this.platformSuperadmin();
      if (user) {
        return `${user.firstName} ${user.lastName}`;
      }
      return 'SuperAdmin';
    }

    if (this.authFacade.status() === 'loading' && !this.tenantUser()) {
      return this.t('shell.loading');
    }

    const user = this.tenantUser();
    if (user) {
      return `${user.firstName} ${user.lastName}`;
    }
    return this.t('shell.user');
  }

  getEmail(): string {
    if (this.isPlatformRoute) {
      return this.platformSuperadmin()?.email || 'superadmin@hrcore.local';
    }

    if (this.authFacade.status() === 'loading' && !this.tenantUser()) {
      return this.t('shell.loading');
    }

    return this.tenantUser()?.email || '';
  }

  getInitials(): string {
    if (this.isPlatformRoute) {
      const user = this.platformSuperadmin();
      if (user) {
        return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
      }
      return 'SA';
    }

    const user = this.tenantUser();
    if (user) {
      return `${user.firstName?.charAt(0) ?? ''}${user.lastName?.charAt(0) ?? ''}`.toUpperCase() || 'US';
    }
    return 'US';
  }

  getProfilePhotoUrl(): string | null {
    if (this.isPlatformRoute) {
      return null;
    }
    return this.profilePhotoSource();
  }

  ngOnDestroy(): void {
    this.clearProfilePhotoSource();
  }

  private async loadProfilePhoto(photoUrl: string, requestKey: string): Promise<void> {
    try {
      const photo = await firstValueFrom(this.authService.getProfilePhoto());
      if (this.profilePhotoRequestKey !== requestKey || !photoUrl) {
        return;
      }

      this.clearProfilePhotoSource();
      this.profilePhotoObjectUrl = URL.createObjectURL(photo);
      this.profilePhotoSource.set(this.profilePhotoObjectUrl);
    } catch {
      if (this.profilePhotoRequestKey === requestKey) {
        this.clearProfilePhotoSource();
      }
    }
  }

  private clearProfilePhotoSource(): void {
    if (this.profilePhotoObjectUrl) {
      URL.revokeObjectURL(this.profilePhotoObjectUrl);
      this.profilePhotoObjectUrl = null;
    }
    this.profilePhotoSource.set(null);
  }

  getRole(): string {
    if (this.isPlatformRoute) {
      return 'Super Administrador';
    }

    if (this.isOwnerAdmin) return this.t('shell.owner');
    const roles = this.tenantUser()?.roles ?? [];
    return roles.length > 0 ? roles.join(', ') : this.t('shell.member');
  }

  toggleProfileMenu(): void {
    this.isProfileOpen = !this.isProfileOpen;
    if (this.isProfileOpen) {
      this.isNotificationsOpen = false;
    }
  }

  toggleNotificationsMenu(): void {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    if (this.isNotificationsOpen) {
      this.isProfileOpen = false;
      void this.loadNotificationPreview();
    }
  }

  async loadNotificationPreview(): Promise<void> {
    if (this.isPlatformRoute) {
      return;
    }

    this.notificationsLoading = true;
    try {
      const [countResponse, listResponse] = await Promise.all([
        firstValueFrom(this.notificationsService.unreadCount()),
        firstValueFrom(this.notificationsService.listNotifications(0, 5))
      ]);

      if (countResponse.success && countResponse.data) {
        this.unreadNotifications.set(countResponse.data.unreadCount);
      }

      if (listResponse.success && listResponse.data) {
        this.notificationPreview.set(listResponse.data.content ?? []);
      }
    } catch {
      this.unreadNotifications.set(0);
      this.notificationPreview.set([]);
    } finally {
      this.notificationsLoading = false;
    }
  }

  async markAllAsRead(): Promise<void> {
    try {
      await firstValueFrom(this.notificationsService.markAllAsRead());
      await this.loadNotificationPreview();
    } catch {
      // Global handler se encarga del error
    }
  }

  openNotification(notification: NotificationResponse): void {
    this.isNotificationsOpen = false;
    void this.router.navigate(['/dashboard/notifications'], {
      queryParams: { id: notification.id }
    });
  }

  goToNotifications(): void {
    this.isNotificationsOpen = false;
    void this.router.navigate(['/dashboard/notifications']);
  }

  toggleGroup(id: string): void {
    // noop in header
  }

  toggleMobileMenu(): void {
    // noop in header
  }

  closeMobileMenu(): void {
    // noop in header
  }

  onLogout(): void {
    this.isProfileOpen = false;
    this.logoutAction.emit();
  }

  @HostListener('document:click', ['$event.target'])
  onClickOutside(targetElement: EventTarget | null): void {
    if (!targetElement) return;
    const clickedInside = this.elementRef.nativeElement.contains(targetElement as Node);
    if (!clickedInside) {
      this.isProfileOpen = false;
      this.isNotificationsOpen = false;
    }
  }
}
