import { Component, effect, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppearanceSettings, AppearanceSettingsService } from '../../entities/company/api/appearance-settings.service';
import { TenantAppearanceStateService } from '../../entities/company/model/tenant-appearance-state.service';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { TenantLanguageService } from '../../entities/company/api/tenant-language.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { SidebarComponent, HeaderComponent } from '../../widgets/layoutAdmin';
import { APPEARANCE_PRESETS, isHexColor, mix, readableAccent, readableText } from '../appearance-settings-page/appearance-palette';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet, SidebarComponent, HeaderComponent],

  template: `
    <div class="hrcore-brand-theme flex h-screen w-full overflow-hidden bg-accent" [ngStyle]="appearanceStyles">
      <app-sidebar [scope]="'TENANT'" [ngStyle]="sidebarStyles" class="w-0 md:w-64 shrink-0 transition-all duration-300 relative z-50" (logoutAction)="logout()"></app-sidebar>

      <main class="relative flex min-w-0 flex-1 flex-col overflow-hidden">
        <app-header [scope]="'TENANT'" (logoutAction)="logout()"></app-header>

        <div class="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `
})
export class DashboardPageComponent implements OnDestroy {
  private readonly authFacade = inject(AuthFacade);
  private readonly authStorage = inject(AuthStorageService);
  private readonly appearanceApi = inject(AppearanceSettingsService);
  private readonly appearanceState = inject(TenantAppearanceStateService);
  private readonly languageApi = inject(TenantLanguageService);
  private readonly languageState = inject(TenantLanguageStateService);
  private activeIdentity: string | null = null;
  private readonly layoutGeneration: number;
  private readonly languageLayoutGeneration: number;
  private loadSubscription: Subscription | null = null;
  private languageLoadSubscription: Subscription | null = null;

  constructor() {
    this.layoutGeneration = this.appearanceState.enterLayout();
    this.languageLayoutGeneration = this.languageState.enterLayout();
    effect(() => {
      const user = this.authFacade.currentUser();
      const status = this.authFacade.status();
      const session = this.authStorage.getSession();
      const sessionIdentity = session?.tenantSlug ? `${session.loginId}:${session.tenantSlug}` : null;
      // A same-session identity refresh must not clear a confirmed PUT in flight.
      const identity = status === 'ready' && user?.tenantSlug === session?.tenantSlug
        ? sessionIdentity : status === 'loading' && sessionIdentity === this.activeIdentity
          ? this.activeIdentity : null;
      if (identity === this.activeIdentity) return;
      this.activeIdentity = identity;
      this.loadSubscription?.unsubscribe();
      this.loadSubscription = null;
      this.languageLoadSubscription?.unsubscribe();
      this.languageLoadSubscription = null;
      this.appearanceState.activate(identity);
      this.languageState.activate(identity);
      if (!identity) return;
      const revision = this.appearanceState.getRevision();
      this.loadSubscription = this.appearanceApi.get().subscribe({
        next: response => {
          if (this.activeIdentity === identity && this.validAppearance(response.data)) {
            this.appearanceState.confirmFetched(identity, revision, response.data);
          }
        },
        error: () => { /* Keep the original theme when the tenant GET fails. */ },
      });
      const languageRevision = this.languageState.getRevision();
      this.languageLoadSubscription = this.languageApi.get().subscribe({
        next: response => {
          if (this.activeIdentity === identity
            && !this.languageState.confirmFetched(identity, languageRevision, response.data?.language)) {
            this.languageState.failFetched(identity, languageRevision);
          }
        },
        error: () => this.languageState.failFetched(identity, languageRevision),
      });
    });
  }

  get appearanceStyles(): Record<string, string> {
    const appearance = this.appearanceState.confirmedForCurrentSession();
    if (!appearance) return {};
    // The same token is used for buttons and text throughout the existing UI.
    // Keep it readable against light cards without changing shared components.
    const primary = readableAccent(appearance.primary, '#ffffff', '#1f2937');
    const secondary = readableAccent(appearance.secondary, '#ffffff', '#1f2937');
    return {
      '--primary': primary,
      '--color-primary': primary,
      '--primary-foreground': readableText(primary),
      '--color-primary-foreground': readableText(primary),
      '--secondary': secondary,
      '--color-secondary': secondary,
      '--secondary-foreground': readableText(secondary),
      '--color-secondary-foreground': readableText(secondary),
      '--sidebar': appearance.sidebar,
      '--color-sidebar': appearance.sidebar,
      '--sidebar-foreground': readableText(appearance.sidebar),
      '--color-sidebar-foreground': readableText(appearance.sidebar),
      '--sidebar-primary': primary,
      '--color-sidebar-primary': primary,
      '--sidebar-primary-foreground': readableText(primary),
      '--color-sidebar-primary-foreground': readableText(primary),
      '--sidebar-ring': secondary,
      '--color-sidebar-ring': secondary,
      '--ring': secondary,
      '--color-ring': secondary,
    };
  }

  get sidebarStyles(): Record<string, string> {
    const appearance = this.appearanceState.confirmedForCurrentSession();
    if (!appearance) return {};
    const foreground = readableText(appearance.sidebar);
    const active = readableAccent(appearance.primary, appearance.sidebar, foreground);
    return {
      '--card': appearance.sidebar,
      '--color-card': appearance.sidebar,
      '--card-foreground': foreground,
      '--color-card-foreground': foreground,
      '--foreground': foreground,
      '--color-foreground': foreground,
      '--muted-foreground': foreground,
      '--color-muted-foreground': foreground,
      '--primary': active,
      '--color-primary': active,
      '--primary-foreground': readableText(active),
      '--color-primary-foreground': readableText(active),
      '--accent': mix(appearance.sidebar, foreground, 0.15),
      '--color-accent': mix(appearance.sidebar, foreground, 0.15),
      '--accent-foreground': foreground,
      '--color-accent-foreground': foreground,
      '--border': mix(appearance.sidebar, foreground, 0.25),
      '--color-border': mix(appearance.sidebar, foreground, 0.25),
    };
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.languageLoadSubscription?.unsubscribe();
    this.appearanceState.deactivate(this.layoutGeneration);
    this.languageState.deactivate(this.languageLayoutGeneration);
  }

  logout(): void {
    this.loadSubscription?.unsubscribe();
    this.languageLoadSubscription?.unsubscribe();
    this.appearanceState.deactivate(this.layoutGeneration);
    this.languageState.deactivate(this.languageLayoutGeneration);
    this.activeIdentity = null;
    void this.authFacade.logout();
  }

  private validAppearance(value: AppearanceSettings | null | undefined): value is AppearanceSettings {
    return !!value && APPEARANCE_PRESETS.some(preset => preset.id === value.preset)
      && (value.mode === 'light' || value.mode === 'dark')
      && isHexColor(value.primary) && isHexColor(value.secondary) && isHexColor(value.sidebar);
  }
}
