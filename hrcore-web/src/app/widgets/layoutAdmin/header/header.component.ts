import { Component, EventEmitter, Output, HostListener, ElementRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { trigger, transition, style, animate } from '@angular/animations';
import { firstValueFrom } from 'rxjs';
import { AuthFacade } from '../../../shared/lib/auth/auth.facade';
import { PlatformFacade } from '../../../features/platform/lib/platform.facade';
import { PermissionService } from '../../../shared/lib/auth/permission.service';
import { NotificationsService, NotificationResponse } from '../../../entities/notifications';
import { environment } from '../../../../environments/environment';

interface MenuItem {
  label: string;
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
        style({ opacity: 0, transform: 'scale(0.95) translateY(-10px)' }),
        animate('150ms cubic-bezier(0.4, 0, 0.2, 1)', style({ opacity: 1, transform: 'scale(1) translateY(0)' }))
      ]),
      transition(':leave', [
        animate('150ms cubic-bezier(0.4, 0, 0.2, 1)', style({ opacity: 0, transform: 'scale(0.95) translateY(-10px)' }))
      ])
    ])
  ],
  template: `
    <header class="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-[#C8E6C9] bg-white px-6 shadow-sm md:px-8">
      <div class="flex items-center gap-4">
        <h1 class="ml-10 text-xl font-semibold tracking-tight text-[#2E7D32] md:ml-0">
          @if (isPlatformRoute) {
            Panel de Control - Plataforma
          } @else if (isOwnerAdmin) {
            Panel de Control
          } @else {
            Panel de Control
          }
        </h1>
      </div>

      <div class="relative flex items-center gap-2 md:gap-4">
        @if (!isPlatformRoute) {
          <div class="relative">
            <button
              type="button"
              (click)="toggleNotificationsMenu()"
              class="relative flex cursor-pointer items-center justify-center rounded-full p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]"
              [class.bg-[#F1F8E9]]="isNotificationsOpen">
              <lucide-icon name="bell" class="h-5 w-5"></lucide-icon>
              @if (unreadNotifications() > 0) {
                <span class="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#C62828] px-1 text-[10px] font-bold text-white">
                  {{ unreadNotifications() }}
                </span>
              }
            </button>

            @if (isNotificationsOpen) {
              <div
                @dropdownAnimation
                class="absolute right-0 mt-2 w-96 max-w-[calc(100vw-1.5rem)] overflow-hidden rounded-2xl border border-[#C8E6C9] bg-white shadow-lg z-50">
                <div class="flex items-center justify-between border-b border-[#C8E6C9] bg-[#F1F8E9] px-4 py-3">
                  <div>
                    <p class="text-sm font-semibold text-[#2E7D32]">Notificaciones</p>
                    <p class="text-xs text-[#666666]">{{ unreadNotifications() }} sin leer</p>
                  </div>
                  <button
                    type="button"
                    (click)="markAllAsRead()"
                    class="cursor-pointer rounded-full border border-[#C8E6C9] bg-white px-3 py-1 text-xs font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                    Marcar todo
                  </button>
                </div>

                <div class="max-h-96 overflow-y-auto">
                  @if (notificationsLoading) {
                    <div class="flex items-center justify-center gap-3 p-6 text-sm text-[#6B7D6C]">
                      <lucide-icon name="loader-2" [size]="16" class="animate-spin"></lucide-icon>
                      Cargando...
                    </div>
                  } @else {
                    @for (notification of notificationPreview(); track notification.id) {
                      <button
                        type="button"
                        (click)="openNotification(notification)"
                        class="flex w-full cursor-pointer gap-3 border-b border-[#EEF5EA] px-4 py-3 text-left transition-colors hover:bg-[#F7FBF3]">
                        <div class="mt-1 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#2E7D32]/10 text-[#2E7D32]">
                          <lucide-icon name="bell" [size]="16"></lucide-icon>
                        </div>
                        <div class="min-w-0 flex-1">
                          <div class="flex items-center justify-between gap-2">
                            <p class="truncate text-sm font-semibold text-[#1B5E20]">{{ notification.title }}</p>
                            @if (notification.status === 'UNREAD') {
                              <span class="shrink-0 rounded-full bg-[#2E7D32]/10 px-2 py-0.5 text-[10px] font-semibold text-[#2E7D32]">Nueva</span>
                            }
                          </div>
                          <p class="mt-1 line-clamp-2 text-xs leading-5 text-[#6B7D6C]">{{ notification.body }}</p>
                          <p class="mt-1 text-[11px] text-[#8A9A8B]">{{ notification.createdAt | date:'short' }}</p>
                        </div>
                      </button>
                    } @empty {
                      <div class="p-6 text-center text-sm text-[#6B7D6C]">
                        No hay notificaciones recientes.
                      </div>
                    }
                  }
                </div>

                <div class="border-t border-[#C8E6C9] p-3">
                  <button
                    type="button"
                    (click)="goToNotifications()"
                    class="inline-flex w-full cursor-pointer items-center justify-center gap-2 rounded-full bg-[#2E7D32] px-4 py-2 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-[#256428]">
                    Ver bandeja
                  </button>
                </div>
              </div>
            }
          </div>
        }

        <div class="h-6 w-px bg-[#C8E6C9] mx-1"></div>

        <div class="relative">
          <button 
            (click)="toggleProfileMenu()"
            class="flex cursor-pointer items-center gap-2 rounded-full p-1.5 transition-colors hover:bg-[#F1F8E9] focus:outline-none md:gap-3 md:rounded-md"
            [class.bg-[#F1F8E9]]="isProfileOpen"
          >
            <div class="flex h-8 w-8 overflow-hidden rounded-full border border-[#2E7D32]/20 bg-[#2E7D32]/10 text-sm font-bold uppercase text-[#2E7D32]">
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
            <div class="hidden flex-col items-start text-left md:flex">
              <span class="text-sm font-semibold leading-none text-[#2E7D32]">
                {{ getDisplayName() }}
              </span>
              <span class="text-xs text-[#666666] mt-1 capitalize">
                {{ getRole() }}
              </span>
            </div>
            <lucide-icon 
              name="chevron-down" 
              class="hidden h-4 w-4 text-[#666666] transition-transform duration-200 md:block"
              [class.rotate-180]="isProfileOpen"
            ></lucide-icon>
          </button>

          <div 
            *ngIf="isProfileOpen"
            @dropdownAnimation
            class="absolute right-0 mt-2 w-56 rounded-md shadow-lg bg-white border border-[#C8E6C9] ring-1 ring-black/5 focus:outline-none z-50 overflow-hidden"
          >
            <div class="px-4 py-3 border-b border-[#C8E6C9] bg-[#F1F8E9]">
              <p class="text-sm font-medium text-[#2E7D32]">
                {{ getDisplayName() }}
              </p>
              <p class="text-xs font-medium text-[#666666] truncate">
                {{ getEmail() }}
              </p>
            </div>

            <div class="py-1">
              @if (isPlatformRoute) {
                @for (item of platformMenuOptions; track item.route) {
                  <a
                    [routerLink]="item.route"
                    (click)="isProfileOpen = false"
                    class="group flex cursor-pointer items-center px-4 py-2 text-sm text-[#333333] transition-colors hover:bg-[#F1F8E9]">
                    <lucide-icon [name]="item.icon" class="mr-3 h-4 w-4 text-[#666666] group-hover:text-[#2E7D32]"></lucide-icon>
                    {{ item.label }}
                  </a>
                }
              } @else {
                @for (item of tenantMenuOptions; track item.route) {
                  <a
                    [routerLink]="item.route"
                    (click)="isProfileOpen = false"
                    class="group flex cursor-pointer items-center px-4 py-2 text-sm text-[#333333] transition-colors hover:bg-[#F1F8E9]">
                    <lucide-icon [name]="item.icon" class="mr-3 h-4 w-4 text-[#666666] group-hover:text-[#2E7D32]"></lucide-icon>
                    {{ item.label }}
                  </a>
                }
              }
            </div>

            <div class="border-t border-[#C8E6C9] py-1">
              <button 
                (click)="onLogout()"
                class="group flex w-full cursor-pointer items-center px-4 py-2 text-sm text-[#C62828] transition-colors hover:bg-red-50"
              >
                <lucide-icon name="log-out" class="mr-3 h-4 w-4 text-[#C62828]/70 group-hover:text-[#C62828]"></lucide-icon>
                Cerrar Sesión
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  `
})
export class HeaderComponent implements OnInit {
  @Output() logoutAction = new EventEmitter<void>();
  isProfileOpen = false;
  isNotificationsOpen = false;
  notificationsLoading = false;
  readonly notificationPreview = signal<NotificationResponse[]>([]);
  readonly unreadNotifications = signal(0);

  readonly platformMenuOptions: MenuItem[] = [
    { label: 'Mi perfil', route: '/platform/profile', icon: 'user-circle-2' },
    { label: 'Seguridad', route: '/platform/security', icon: 'lock' },
    { label: 'Configuraciones', route: '/platform/settings', icon: 'settings' },
    { label: 'Respaldos', route: '/platform/backups', icon: 'arrow-down-to-line' }
  ];

  private elementRef = inject(ElementRef);
  private router = inject(Router);
  private authFacade = inject(AuthFacade);
  private platformFacade = inject(PlatformFacade);
  private readonly permissionService = inject(PermissionService);
  private readonly notificationsService = inject(NotificationsService);

  readonly platformSuperadmin = this.platformFacade.currentSuperadmin;
  readonly tenantUser = this.authFacade.currentUser;

  get tenantMenuOptions(): MenuItem[] {
    return [
      { label: 'Dashboard', route: this.dashboardRoute, icon: 'layout-dashboard' },
      { label: 'Mi perfil', route: '/dashboard/profile', icon: 'user-circle-2' },
      { label: 'Notificaciones', route: '/dashboard/notifications', icon: 'bell' }
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
    return this.router.url.startsWith('/platform');
  }

  get isOwnerAdmin(): boolean {
    return this.permissionService.hasRole('OWNER_ADMIN');
  }

  get dashboardRoute(): string {
    return this.isOwnerAdmin ? '/dashboard/summary' : '/dashboard/me';
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
      return 'Cargando...';
    }

    const user = this.tenantUser();
    if (user) {
      return `${user.firstName} ${user.lastName}`;
    }
    return 'Usuario';
  }

  getEmail(): string {
    if (this.isPlatformRoute) {
      return this.platformSuperadmin()?.email || 'superadmin@hrcore.local';
    }

    if (this.authFacade.status() === 'loading' && !this.tenantUser()) {
      return 'Cargando...';
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

    const user = this.tenantUser();
    const profilePhotoUrl = user?.profilePhotoUrl;
    if (!profilePhotoUrl) {
      return null;
    }

    const updatedAt = user?.updatedAt ? `?v=${encodeURIComponent(user.updatedAt)}` : '';
    return `${environment.apiUrl}${profilePhotoUrl}${updatedAt}`;
  }

  getRole(): string {
    if (this.isPlatformRoute) {
      return 'Super Administrador';
    }

    return this.isOwnerAdmin ? 'Owner Admin' : 'Usuario';
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
