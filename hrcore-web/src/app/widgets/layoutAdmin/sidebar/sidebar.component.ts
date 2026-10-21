import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { NavigationScope, PLATFORM_NAVIGATION, TenantNavigationItem } from '../../../shared/lib/navigation';
import { TenantNavigationService } from '../../../shared/lib/navigation/tenant-navigation.service';
import { PlatformFacade } from '../../../features/platform/lib/platform.facade';
import { PermissionService } from '../../../shared/lib/auth/permission.service';
import { TenantLanguageStateService } from '../../../shared/lib/i18n/tenant-language-state.service';
import { TenantTextKey } from '../../../shared/lib/i18n/keys';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <button
      *ngIf="!isMobileOpen"
      type="button"
      (click)="toggleMobileMenu()"
      class="fixed left-4 top-3 z-40 rounded-xl border border-border bg-card p-2 text-primary shadow-sm transition-all duration-200 hover:bg-accent md:hidden">
      <lucide-icon name="menu" class="h-5 w-5" />
    </button>

    <div
      *ngIf="isMobileOpen"
      (click)="closeMobileMenu()"
      class="animate-fade-in fixed inset-0 z-40 bg-sidebar/50 backdrop-blur-xs md:hidden">
    </div>

    <aside
      (mouseenter)="isSidebarHovered = true"
      (mouseleave)="isSidebarHovered = false"
      class="fixed inset-y-0 left-0 z-50 flex h-full flex-col border-r border-border bg-card shadow-xl transition-all duration-300 ease-out md:relative md:translate-x-0"
      [class.w-64]="!isCollapsed"
      [class.w-20]="isCollapsed"
      [class.-translate-x-full]="!isMobileOpen"
      [class.translate-x-0]="isMobileOpen">
      <!-- Brand Header -->
      <div
        class="flex h-16 shrink-0 items-center border-b border-border transition-all duration-300"
        [class.justify-between]="!isCollapsed"
        [class.px-4]="!isCollapsed"
        [class.justify-center]="isCollapsed"
        [class.px-2]="isCollapsed">
        <!-- Modo Expandido: Logo + Nombre HR Core + Botón para contraer a la izquierda -->
        <ng-container *ngIf="!isCollapsed">
          <a
            [routerLink]="scope === 'PLATFORM' ? '/platform/dashboard' : '/dashboard/home'"
            (click)="closeMobileMenu()"
            class="group flex min-w-0 items-center gap-3 transition-transform duration-200 hover:scale-[1.01]">
            <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-border bg-background shadow-2xs transition-all duration-200 group-hover:border-primary/40">
              <img src="/logo.png" alt="HR Core" class="h-7 w-7 rounded-md object-contain">
            </div>
            <div class="min-w-0">
              <span class="block truncate text-base font-black leading-none tracking-tight text-primary">HR Core</span>
              <span class="mt-0.5 block truncate text-[10px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                {{ scope === 'PLATFORM' ? 'Plataforma' : 'Gestión RR. HH.' }}
              </span>
            </div>
          </a>

          <div class="flex items-center gap-1">
            <!-- Botón para contraer el sidebar hacia la izquierda (Desktop) -->
            <button
              type="button"
              (click)="collapseSidebar()"
              title="Contraer menú lateral"
              aria-label="Contraer menú lateral"
              class="hidden md:inline-flex h-8 w-8 shrink-0 cursor-pointer items-center justify-center rounded-xl border border-border/80 bg-background text-primary shadow-2xs transition-all duration-200 hover:-translate-x-0.5 hover:border-primary/40 hover:bg-accent">
              <lucide-icon name="arrow-left" class="h-4 w-4" />
            </button>

            <!-- Botón cerrar en móvil -->
            <button
              type="button"
              (click)="closeMobileMenu()"
              class="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground md:hidden">
              <lucide-icon name="x" class="h-5 w-5" />
            </button>
          </div>
        </ng-container>

        <!-- Modo Contraído (Solo Iconos): El logo principal se transforma en botón para expandir al pasar el cursor por el sidebar -->
        <ng-container *ngIf="isCollapsed">
          <button
            type="button"
            (click)="expandSidebar()"
            title="Expandir menú lateral"
            aria-label="Expandir menú lateral"
            class="group relative flex h-11 w-11 cursor-pointer items-center justify-center rounded-2xl border transition-all duration-200"
            [class.border-primary]="isSidebarHovered"
            [class.bg-primary]="isSidebarHovered"
            [class.text-primary-foreground]="isSidebarHovered"
            [class.shadow-md]="isSidebarHovered"
            [class.scale-105]="isSidebarHovered"
            [class.border-border]="!isSidebarHovered"
            [class.bg-background]="!isSidebarHovered">
            <img
              *ngIf="!isSidebarHovered"
              src="/logo.png"
              alt="HR Core"
              class="h-7 w-7 rounded-md object-contain transition-transform duration-200" />
            <lucide-icon
              *ngIf="isSidebarHovered"
              name="arrow-right"
              class="h-5 w-5 animate-fade-in" />
          </button>
        </ng-container>
      </div>

      <!-- Navegación: Modo Expandido -->
      <nav *ngIf="!isCollapsed" class="flex-1 space-y-1.5 overflow-y-auto px-3 py-4">
        <a
          *ngFor="let item of standaloneItems; trackBy: trackItem"
          [routerLink]="item.route"
          routerLinkActive="bg-primary text-primary-foreground font-bold shadow-sm hover:bg-primary/95 hover:text-primary-foreground"
          [routerLinkActiveOptions]="{ exact: true }"
          (click)="closeMobileMenu()"
          class="group flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-foreground transition-all duration-200 hover:translate-x-0.5 hover:bg-accent hover:text-primary">
          <lucide-icon [name]="item.icon" class="h-4 w-4 shrink-0 transition-transform duration-200 group-hover:scale-110" />
          <span class="truncate">{{ item.label }}</span>
        </a>

        <section *ngFor="let group of navigationGroups; trackBy: trackGroup" class="pt-1.5">
          <button
            type="button"
            class="group flex w-full cursor-pointer items-center justify-between rounded-xl px-3 py-2 text-[11px] font-black uppercase tracking-[0.14em] transition-all duration-200"
            [class.bg-accent/70]="isGroupActive(group)"
            [class.text-primary]="isGroupActive(group) || isGroupExpanded(group.id)"
            [class.text-muted-foreground]="!isGroupActive(group) && !isGroupExpanded(group.id)"
            [class.hover:bg-muted]="!isGroupActive(group)"
            [class.hover:text-foreground]="!isGroupActive(group)"
            [attr.aria-expanded]="isGroupExpanded(group.id)"
            (click)="toggleGroup(group.id)">
            <span class="flex items-center gap-2">
              <lucide-icon [name]="group.icon" class="h-3.5 w-3.5 transition-transform duration-200 group-hover:scale-110" />
              <span>{{ group.label }}</span>
            </span>
            <span class="flex items-center gap-1.5">
              <span class="rounded-full bg-muted px-1.5 py-0.5 text-[10px] font-bold text-muted-foreground">
                {{ group.items.length }}
              </span>
              <lucide-icon
                name="chevron-down"
                class="h-4 w-4 transition-transform duration-300 ease-out"
                [class.rotate-180]="isGroupExpanded(group.id)" />
            </span>
          </button>

          <!-- Contenedor con transición suave CSS Grid -->
          <div
            class="grid transition-all duration-300 ease-in-out"
            [class.grid-rows-[1fr]]="isGroupExpanded(group.id)"
            [class.opacity-100]="isGroupExpanded(group.id)"
            [class.mt-1]="isGroupExpanded(group.id)"
            [class.grid-rows-[0fr]]="!isGroupExpanded(group.id)"
            [class.opacity-0]="!isGroupExpanded(group.id)"
            [class.pointer-events-none]="!isGroupExpanded(group.id)">
            <div class="overflow-hidden">
              <div class="ml-3.5 space-y-1 border-l-2 border-primary/20 py-0.5 pl-2.5">
                <a
                  *ngFor="let item of group.items; trackBy: trackItem"
                  [routerLink]="item.route"
                  routerLinkActive="bg-primary text-primary-foreground font-bold shadow-xs hover:bg-primary/95 hover:text-primary-foreground"
                  [routerLinkActiveOptions]="{ exact: true }"
                  (click)="closeMobileMenu()"
                  class="group flex items-center gap-2.5 rounded-xl px-3 py-2 text-sm font-medium text-foreground transition-all duration-200 hover:translate-x-1 hover:bg-accent hover:text-primary">
                  <lucide-icon [name]="item.icon" class="h-4 w-4 shrink-0 transition-transform duration-200 group-hover:scale-110" />
                  <span class="truncate">{{ item.label }}</span>
                </a>
              </div>
            </div>
          </div>
        </section>
        <div
          *ngIf="showUpgrade"
          class="mx-1 mt-5 rounded-3xl border border-primary/20 bg-gradient-to-br from-accent via-card to-accent p-4 shadow-sm transition-all duration-300 hover:shadow-md">
          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-sm">
              <lucide-icon name="sparkles" class="h-5 w-5" />
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-xs font-black uppercase tracking-[0.18em] text-primary">{{ t('upgrade.eyebrow') }}</p>
              <h3 class="mt-1 text-sm font-black text-accent-foreground">{{ t('upgrade.title') }}</h3>
              <p class="mt-1 text-xs leading-5 text-muted-foreground">{{ t('upgrade.description') }}</p>
            </div>
          </div>
          <a
            routerLink="/dashboard/subscription"
            (click)="closeMobileMenu()"
            class="mt-4 inline-flex h-10 w-full items-center justify-center rounded-2xl bg-primary px-4 text-sm font-black text-primary-foreground shadow-xs transition-all duration-200 hover:-translate-y-0.5 hover:bg-primary/90 hover:shadow-sm">
            {{ t('upgrade.plans') }}
          </a>
        </div>
      </nav>

      <!-- Navegación: Modo Contraído (Solo Loguitos Principales: Inicio y cada Categoría) -->
      <nav *ngIf="isCollapsed" class="flex-1 flex flex-col items-center space-y-2.5 overflow-y-auto px-2 py-4">
        <a
          *ngFor="let item of standaloneItems; trackBy: trackItem"
          [routerLink]="item.route"
          routerLinkActive="bg-primary text-primary-foreground shadow-sm"
          [routerLinkActiveOptions]="{ exact: true }"
          [title]="item.label"
          [attr.aria-label]="item.label"
          class="group relative flex h-11 w-11 items-center justify-center rounded-2xl text-foreground transition-all duration-200 hover:scale-105 hover:bg-accent hover:text-primary">
          <lucide-icon [name]="item.icon" class="h-5 w-5 shrink-0" />
        </a>

        <div class="my-1 h-px w-8 bg-border"></div>

        <button
          *ngFor="let group of navigationGroups; trackBy: trackGroup"
          type="button"
          (click)="openGroupFromCollapsed(group.id)"
          [title]="group.label"
          [attr.aria-label]="group.label"
          class="group relative flex h-11 w-11 cursor-pointer items-center justify-center rounded-2xl transition-all duration-200 hover:scale-105"
          [class.bg-primary]="isGroupActive(group)"
          [class.text-primary-foreground]="isGroupActive(group)"
          [class.shadow-sm]="isGroupActive(group)"
          [class.text-muted-foreground]="!isGroupActive(group)"
          [class.hover:bg-accent]="!isGroupActive(group)"
          [class.hover:text-primary]="!isGroupActive(group)">
          <lucide-icon [name]="group.icon" class="h-5 w-5 shrink-0" />
        </button>
      </nav>

      <!-- Footer -->
      <div class="shrink-0 border-t border-border p-3">
        <button
          *ngIf="!isCollapsed"
          type="button"
          (click)="onLogout()"
          class="group flex w-full cursor-pointer items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-muted-foreground transition-all duration-200 hover:bg-destructive/10 hover:text-destructive">
          <lucide-icon name="log-out" class="h-4 w-4 transition-transform duration-200 group-hover:-translate-x-0.5" />
          <span>{{ scope === 'TENANT' ? t('shell.logout') : 'Cerrar Sesión' }}</span>
        </button>

        <button
          *ngIf="isCollapsed"
          type="button"
          (click)="onLogout()"
          [title]="scope === 'TENANT' ? t('shell.logout') : 'Cerrar Sesión'"
          [attr.aria-label]="scope === 'TENANT' ? t('shell.logout') : 'Cerrar Sesión'"
          class="group mx-auto flex h-11 w-11 cursor-pointer items-center justify-center rounded-2xl text-muted-foreground transition-all duration-200 hover:scale-105 hover:bg-destructive/10 hover:text-destructive">
          <lucide-icon name="log-out" class="h-5 w-5" />
        </button>
      </div>
    </aside>
  `
})
export class SidebarComponent {
  @Input({ required: true }) scope!: NavigationScope;
  @Output() logoutAction = new EventEmitter<void>();
  private readonly tenantNavigation = inject(TenantNavigationService);
  private readonly platformFacade = inject(PlatformFacade);
  private readonly permissions = inject(PermissionService);
  private readonly router = inject(Router);
  private readonly languageState = inject(TenantLanguageStateService);
  isMobileOpen = false;
  isCollapsed = false;
  isSidebarHovered = false;
  private expanded = new Set<string>();
  private explicitlyToggled = new Set<string>();
  private groupsCache: ReturnType<SidebarComponent['buildGroups']> = [];
  private groupsCacheItems: TenantNavigationItem[] | null = null;
  private groupsCacheScope: NavigationScope | null = null;
  readonly groupDefinitions: Record<NavigationScope, { id: string; label: string; icon: string; ids: string[] }[]> = {
    TENANT: [
      { id: 'organization', label: 'Organización', icon: 'building-2', ids: ['users', 'departments', 'positions', 'organization-chart', 'my-team', 'employees'] },
      { id: 'operations', label: 'Gestión laboral', icon: 'briefcase', ids: ['contracts', 'dossiers', 'shifts', 'attendance', 'absences', 'incidents'] },
      { id: 'personal', label: 'Mi espacio', icon: 'user-circle-2', ids: ['profile', 'my-employment', 'notifications'] },
      { id: 'administration', label: 'Administración', icon: 'shield-check', ids: ['roles', 'permissions', 'subscription', 'hr-reports', 'company-settings', 'appearance-settings', 'audit', 'backups', 'security', 'settings'] }
    ],
    PLATFORM: [
      { id: 'platform', label: 'Plataforma', icon: 'building-2', ids: ['tenants', 'plans', 'subscriptions', 'audit', 'backups'] },
      { id: 'account', label: 'Cuenta', icon: 'user-circle-2', ids: ['profile', 'security', 'settings'] }
    ]
  };
  get allVisibleItems(): TenantNavigationItem[] { return this.scope === 'PLATFORM' ? (this.platformFacade.currentSuperadmin() ? PLATFORM_NAVIGATION : []) : this.tenantNavigation.visibleItems(); }
  get standaloneItems(): TenantNavigationItem[] { const grouped = new Set(this.groupDefinitions[this.scope].flatMap(group => group.ids)); return this.allVisibleItems.filter(item => !grouped.has(item.id)); }
  get navigationGroups() { return this.buildGroups(this.allVisibleItems); }
  private buildGroups(items: TenantNavigationItem[]) { return this.groupDefinitions[this.scope].map(group => ({ ...group, label: this.scope === 'TENANT' ? this.t(`group.${group.id}` as TenantTextKey) : group.label, items: group.ids.map(id => items.find(item => item.id === id)).filter((item): item is TenantNavigationItem => !!item) })).filter(group => group.items.length); }
  t(key: TenantTextKey): string { return this.languageState.t(key); }
  isGroupActive(group: { items: TenantNavigationItem[] }): boolean {
    return group.items.some(item => this.router.url.startsWith(item.route));
  }
  isGroupExpanded(id: string): boolean {
    if (this.explicitlyToggled.has(id)) {
      return this.expanded.has(id);
    }
    const group = this.navigationGroups.find(item => item.id === id);
    if (group && this.isGroupActive(group)) {
      return true;
    }
    return this.navigationGroups[0]?.id === id && !this.navigationGroups.some(g => this.isGroupActive(g));
  }
  trackGroup(_: number, group: { id: string }): string { return group.id; }
  trackItem(_: number, item: TenantNavigationItem): string { return item.id; }
  toggleGroup(id: string): void {
    const currentlyExpanded = this.isGroupExpanded(id);
    this.explicitlyToggled.add(id);
    if (currentlyExpanded) {
      this.expanded.delete(id);
    } else {
      this.expanded.add(id);
    }
  }
  get showUpgrade(): boolean { return this.scope === 'TENANT' && this.permissions.isCurrentOwner() && this.permissions.hasPermission('billing.subscription.manage'); }
  collapseSidebar(): void {
    this.isCollapsed = true;
    this.isSidebarHovered = false;
  }
  expandSidebar(): void {
    this.isCollapsed = false;
    this.isSidebarHovered = false;
  }
  openGroupFromCollapsed(groupId: string): void {
    this.isCollapsed = false;
    this.isSidebarHovered = false;
    this.explicitlyToggled.add(groupId);
    this.expanded.add(groupId);
  }
  toggleMobileMenu(): void { this.isMobileOpen = !this.isMobileOpen; }
  closeMobileMenu(): void { this.isMobileOpen = false; }
  onLogout(): void { this.logoutAction.emit(); }
}
