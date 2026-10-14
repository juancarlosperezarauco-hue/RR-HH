import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { NavigationScope, PLATFORM_NAVIGATION, TenantNavigationItem } from '../../../shared/lib/navigation';
import { TenantNavigationService } from '../../../shared/lib/navigation/tenant-navigation.service';
import { PlatformFacade } from '../../../features/platform/lib/platform.facade';
import { PermissionService } from '../../../shared/lib/auth/permission.service';

@Component({
  selector: 'app-sidebar', standalone: true, imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <button *ngIf="!isMobileOpen" (click)="toggleMobileMenu()" class="md:hidden fixed top-3 left-4 z-40 p-2 bg-card border border-border rounded-md shadow-sm text-primary"><lucide-icon name="menu" class="h-5 w-5" /></button>
    <div *ngIf="isMobileOpen" (click)="closeMobileMenu()" class="md:hidden fixed inset-0 bg-sidebar/50 z-40"></div>
    <aside class="fixed inset-y-0 left-0 z-50 flex h-full w-64 flex-col border-r border-border bg-card shadow-xl md:relative md:translate-x-0" [class.-translate-x-full]="!isMobileOpen" [class.translate-x-0]="isMobileOpen">
      <div class="flex h-16 shrink-0 items-center justify-between border-b border-border px-6"><div class="flex items-center gap-3"><img src="/logo.png" alt="HR Core" class="h-8 w-8 rounded-md object-contain"><span class="font-bold text-lg tracking-tight text-primary">HR Core</span></div><button (click)="closeMobileMenu()" class="md:hidden p-1.5"><lucide-icon name="x" class="h-5 w-5" /></button></div>
      <nav class="flex-1 space-y-1 overflow-y-auto px-3 py-4"><a *ngFor="let item of visibleItems" [routerLink]="item.route" routerLinkActive="sidebar-active bg-primary text-primary-foreground font-semibold" [routerLinkActiveOptions]="{exact: true}" (click)="closeMobileMenu()" class="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-foreground hover:bg-accent hover:text-primary"><lucide-icon [name]="item.icon" class="h-4 w-4" />{{ item.label }}</a>
        <div *ngIf="showUpgrade" class="mx-1 mt-5 rounded-3xl border border-border/30 bg-gradient-to-br from-accent via-card to-accent p-4 shadow-sm"><div class="flex items-start gap-3"><div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-sm"><lucide-icon name="sparkles" class="h-5 w-5" /></div><div class="min-w-0 flex-1"><p class="text-xs font-black uppercase tracking-[0.18em] text-primary">Upgrade</p><h3 class="mt-1 text-sm font-black text-accent-foreground">Mejora tu plan</h3><p class="mt-1 text-xs leading-5 text-muted-foreground">Amplía la capacidad de usuarios y permisos de tu tenant.</p></div></div><a routerLink="/dashboard/subscription" (click)="closeMobileMenu()" class="mt-4 inline-flex h-10 w-full items-center justify-center rounded-2xl bg-primary px-4 text-sm font-black text-primary-foreground hover:bg-primary/90">Ver planes</a></div>
      </nav><div class="shrink-0 border-t border-border p-4"><button (click)="onLogout()" class="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-muted-foreground hover:bg-destructive/5 hover:text-destructive"><lucide-icon name="log-out" class="h-4 w-4" />Cerrar Sesión</button></div>
    </aside>`
})
export class SidebarComponent {
  @Input({ required: true }) scope!: NavigationScope;
  @Output() logoutAction = new EventEmitter<void>();
  private readonly tenantNavigation = inject(TenantNavigationService);
  private readonly platformFacade = inject(PlatformFacade);
  private readonly permissions = inject(PermissionService);
  isMobileOpen = false;
<<<<<<< HEAD
  get visibleItems(): TenantNavigationItem[] { return this.scope === 'PLATFORM' ? (this.platformFacade.currentSuperadmin() ? PLATFORM_NAVIGATION : []) : this.tenantNavigation.visibleItems(); }
  get showUpgrade(): boolean { return this.scope === 'TENANT' && this.permissions.isCurrentOwner() && this.permissions.hasPermission('billing.subscription.manage'); }
=======

  readonly tenantItems: MenuItem[] = [
    { label: 'Usuarios', route: '/dashboard/users', icon: 'users' },
    { label: 'Contratos', route: '/dashboard/contracts', icon: 'file-signature' },
    { label: 'Departamentos', route: '/dashboard/departments', icon: 'building-2', permission: 'departments.list' },
    { label: 'Cargos', route: '/dashboard/positions', icon: 'briefcase', permission: 'positions.list' },
    { label: 'Roles', route: '/dashboard/roles', icon: 'key' },
    { label: 'Permisos', route: '/dashboard/permissions', icon: 'badge-check' },
    { label: 'Expedientes', route: '/dashboard/dossiers', icon: 'file-text' },
    { label: 'Turnos', route: '/dashboard/shifts', icon: 'clock' },
    { label: 'Asistencia', route: '/dashboard/attendance', icon: 'clipboard-list' },
    { label: 'Ausencias', route: '/dashboard/absences', icon: 'calendar' },
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
  get visibleItems(): MenuItem[] { return this.router.url.startsWith('/platform') ? this.platformItems : this.tenantItems.filter(item => !item.permission || this.permissions.hasPermission(item.permission)); }
>>>>>>> main
  toggleMobileMenu(): void { this.isMobileOpen = !this.isMobileOpen; }
  closeMobileMenu(): void { this.isMobileOpen = false; }
  onLogout(): void { this.logoutAction.emit(); }
}
