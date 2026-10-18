import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
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
      <nav class="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        <a *ngFor="let item of standaloneItems; trackBy: trackItem" [routerLink]="item.route" routerLinkActive="bg-primary text-primary-foreground font-semibold" [routerLinkActiveOptions]="{exact: true}" (click)="closeMobileMenu()" class="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-foreground hover:bg-accent hover:text-primary"><lucide-icon [name]="item.icon" class="h-4 w-4" />{{ item.label }}</a>
        <section *ngFor="let group of navigationGroups; trackBy: trackGroup" class="pt-2">
          <button type="button" class="flex w-full items-center justify-between rounded-md px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-muted-foreground hover:bg-muted hover:text-foreground" [attr.aria-expanded]="isGroupExpanded(group.id)" (click)="toggleGroup(group.id)"><span class="flex items-center gap-2"><lucide-icon [name]="group.icon" class="h-3.5 w-3.5" />{{ group.label }}</span><lucide-icon name="chevron-down" class="h-4 w-4 transition-transform" [class.rotate-180]="isGroupExpanded(group.id)" /></button>
          <div *ngIf="isGroupExpanded(group.id)" class="mt-1 space-y-1 border-l border-border pl-2">
            <a *ngFor="let item of group.items; trackBy: trackItem" [routerLink]="item.route" routerLinkActive="bg-primary text-primary-foreground font-semibold" [routerLinkActiveOptions]="{exact: true}" (click)="closeMobileMenu()" class="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-foreground hover:bg-accent hover:text-primary"><lucide-icon [name]="item.icon" class="h-4 w-4" />{{ item.label }}</a>
          </div>
        </section>
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
  private readonly router = inject(Router);
  isMobileOpen = false;
  private expanded = new Set<string>();
  private groupsCache: ReturnType<SidebarComponent['buildGroups']> = [];
  private groupsCacheItems: TenantNavigationItem[] | null = null;
  private groupsCacheScope: NavigationScope | null = null;
  readonly groupDefinitions: Record<NavigationScope, { id: string; label: string; icon: string; ids: string[] }[]> = {
    TENANT: [
      { id: 'organization', label: 'Organización', icon: 'building-2', ids: ['users', 'departments', 'positions', 'organization-chart', 'my-team', 'employees'] },
      { id: 'operations', label: 'Gestión laboral', icon: 'briefcase', ids: ['contracts', 'dossiers', 'shifts', 'attendance', 'absences', 'incidents'] },
      { id: 'personal', label: 'Mi espacio', icon: 'user-circle-2', ids: ['profile', 'my-employment', 'notifications'] },
      { id: 'administration', label: 'Administración', icon: 'shield-check', ids: ['roles', 'permissions', 'subscription', 'hr-reports', 'company-settings', 'audit', 'backups', 'security', 'settings'] }
    ],
    PLATFORM: [
      { id: 'platform', label: 'Plataforma', icon: 'building-2', ids: ['tenants', 'plans', 'subscriptions', 'audit', 'backups'] },
      { id: 'account', label: 'Cuenta', icon: 'user-circle-2', ids: ['profile', 'security', 'settings'] }
    ]
  };
  get allVisibleItems(): TenantNavigationItem[] { return this.scope === 'PLATFORM' ? (this.platformFacade.currentSuperadmin() ? PLATFORM_NAVIGATION : []) : this.tenantNavigation.visibleItems(); }
  get standaloneItems(): TenantNavigationItem[] { const grouped = new Set(this.groupDefinitions[this.scope].flatMap(group => group.ids)); return this.allVisibleItems.filter(item => !grouped.has(item.id)); }
  get navigationGroups() { const items = this.allVisibleItems; if (this.groupsCacheItems === items && this.groupsCacheScope === this.scope) return this.groupsCache; this.groupsCacheItems = items; this.groupsCacheScope = this.scope; this.groupsCache = this.buildGroups(items); return this.groupsCache; }
  private buildGroups(items: TenantNavigationItem[]) { return this.groupDefinitions[this.scope].map(group => ({ ...group, items: group.ids.map(id => items.find(item => item.id === id)).filter((item): item is TenantNavigationItem => !!item) })).filter(group => group.items.length); }
  isGroupExpanded(id: string): boolean { if (this.expanded.has(id)) return true; const group = this.navigationGroups.find(item => item.id === id); return !!group?.items.some(item => this.router.url.startsWith(item.route)); }
  trackGroup(_: number, group: { id: string }): string { return group.id; }
  trackItem(_: number, item: TenantNavigationItem): string { return item.id; }
  toggleGroup(id: string): void { if (this.expanded.has(id)) this.expanded.delete(id); else this.expanded.add(id); }
  get showUpgrade(): boolean { return this.scope === 'TENANT' && this.permissions.isCurrentOwner() && this.permissions.hasPermission('billing.subscription.manage'); }
  toggleMobileMenu(): void { this.isMobileOpen = !this.isMobileOpen; }
  closeMobileMenu(): void { this.isMobileOpen = false; }
  onLogout(): void { this.logoutAction.emit(); }
}
