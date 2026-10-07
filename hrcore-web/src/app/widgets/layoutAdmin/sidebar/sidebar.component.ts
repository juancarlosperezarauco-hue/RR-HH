import { Component, inject, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

interface MenuItem { label: string; route: string; icon: string; }

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <button *ngIf="!isMobileOpen" (click)="toggleMobileMenu()" class="md:hidden fixed top-3 left-4 z-40 p-2 bg-white border border-[#C8E6C9] rounded-md shadow-sm text-[#2E7D32]"><lucide-icon name="menu" class="h-5 w-5"></lucide-icon></button>
    <div *ngIf="isMobileOpen" (click)="closeMobileMenu()" class="md:hidden fixed inset-0 bg-black/50 z-40"></div>
    <aside class="fixed inset-y-0 left-0 z-50 flex h-full w-64 flex-col border-r border-[#C8E6C9] bg-white shadow-xl md:relative md:translate-x-0" [class.-translate-x-full]="!isMobileOpen" [class.translate-x-0]="isMobileOpen">
      <div class="flex h-16 shrink-0 items-center justify-between border-b border-[#C8E6C9] px-6"><div class="flex items-center gap-3"><img src="/logo.png" alt="HR Core" class="h-8 w-8 rounded-md object-contain"><span class="font-bold text-lg tracking-tight text-[#2E7D32]">HR Core</span></div><button (click)="closeMobileMenu()" class="md:hidden p-1.5"><lucide-icon name="x" class="h-5 w-5"></lucide-icon></button></div>
      <nav class="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        <a *ngFor="let item of visibleItems" [routerLink]="item.route" routerLinkActive="bg-[#2E7D32] text-white font-semibold" [routerLinkActiveOptions]="{exact: true}" (click)="closeMobileMenu()" class="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-[#333333] hover:bg-[#F1F8E9] hover:text-[#2E7D32]"><lucide-icon [name]="item.icon" class="h-4 w-4"></lucide-icon>{{ item.label }}</a>
        <div *ngIf="!isPlatformRoute" class="mx-1 mt-5 rounded-3xl border border-emerald-100 bg-gradient-to-br from-emerald-50 via-white to-emerald-50 p-4 shadow-sm">
          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-emerald-700 text-white shadow-sm">
              <lucide-icon name="sparkles" class="h-5 w-5"></lucide-icon>
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-xs font-black uppercase tracking-[0.18em] text-emerald-700">Upgrade</p>
              <h3 class="mt-1 text-sm font-black text-[#1B5E20]">Mejora tu plan</h3>
              <p class="mt-1 text-xs leading-5 text-[#567157]">Amplía la capacidad de usuarios y permisos de tu tenant.</p>
            </div>
          </div>
          <a routerLink="/dashboard/subscription" (click)="closeMobileMenu()" class="mt-4 inline-flex h-10 w-full items-center justify-center rounded-2xl bg-emerald-700 px-4 text-sm font-black text-white hover:bg-emerald-800">Ver planes</a>
        </div>
      </nav>
      <div class="shrink-0 border-t border-[#C8E6C9] p-4"><button (click)="onLogout()" class="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-[#666666] hover:bg-red-50 hover:text-red-600"><lucide-icon name="log-out" class="h-4 w-4"></lucide-icon>Cerrar Sesión</button></div>
    </aside>
  `
})
export class SidebarComponent {
  @Output() logoutAction = new EventEmitter<void>();
  private readonly router = inject(Router);
  isMobileOpen = false;

  readonly tenantItems: MenuItem[] = [
    { label: 'Usuarios', route: '/dashboard/users', icon: 'users' },
    { label: 'Roles', route: '/dashboard/roles', icon: 'key' },
    { label: 'Permisos', route: '/dashboard/permissions', icon: 'badge-check' },
    { label: 'Suscripción', route: '/dashboard/subscription', icon: 'credit-card' },
    { label: 'Perfil', route: '/dashboard/profile', icon: 'user-circle-2' },
    // { label: 'Notificaciones', route: '/dashboard/notifications', icon: 'bell' },
    // { label: 'Backups', route: '/dashboard/backups', icon: 'archive' },
    { label: 'Configuración', route: '/dashboard/settings', icon: 'settings' }
  ];
  readonly platformItems: MenuItem[] = [
    { label: 'Dashboard', route: '/platform/dashboard', icon: 'layout-dashboard' },
    { label: 'Planes', route: '/platform/plans', icon: 'credit-card' },
    { label: 'Tenants', route: '/platform/tenants', icon: 'building-2' },
    { label: 'Suscripciones', route: '/platform/subscriptions', icon: 'dollar-sign' },
    { label: 'Auditoría', route: '/platform/audit', icon: 'clipboard-list' },
    // { label: 'Respaldos', route: '/platform/backups', icon: 'archive' },
    { label: 'Perfil', route: '/platform/profile', icon: 'user-circle-2' },
    { label: 'Seguridad', route: '/platform/security', icon: 'lock' },
    { label: 'Configuración', route: '/platform/settings', icon: 'settings' }
  ];
  get isPlatformRoute(): boolean { return this.router.url.startsWith('/platform'); }
  get visibleItems(): MenuItem[] { return this.router.url.startsWith('/platform') ? this.platformItems : this.tenantItems; }
  toggleMobileMenu(): void { this.isMobileOpen = !this.isMobileOpen; }
  closeMobileMenu(): void { this.isMobileOpen = false; }
  onLogout(): void { this.logoutAction.emit(); }
}
