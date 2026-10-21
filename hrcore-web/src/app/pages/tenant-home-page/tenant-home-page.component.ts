import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { TenantNavigationService } from '../../shared/lib/navigation/tenant-navigation.service';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { TenantTextKey } from '../../shared/lib/i18n/keys';

@Component({
  selector: 'app-tenant-home-page', standalone: true, imports: [CommonModule, RouterModule],
  template: `
    <div class="space-y-6">
      @if (auth.status() === 'loading' || auth.status() === 'idle') {
        <section class="rounded-3xl border border-border bg-card p-8 text-sm text-muted-foreground">{{ t('home.checkingSession') }}</section>
      } @else if (auth.currentUser(); as user) {
        <section class="rounded-3xl border border-border bg-card p-6 shadow-sm">
          <p class="text-sm font-semibold text-primary">{{ t('nav.home') }}</p>
          <h1 class="mt-2 text-3xl font-black text-accent-foreground">{{ t('home.greeting') }} {{ user.firstName }}</h1>
          <p class="mt-2 text-sm text-muted-foreground">{{ user.email }} · {{ t('home.organization') }} {{ user.tenantSlug }}</p>
          <p class="mt-3 text-sm text-muted-foreground">{{ permissions.isCurrentOwner() ? t('shell.owner') : t('home.collaborator') }}</p>
        </section>
        <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (item of navigation.visibleItems(); track item.id) {
            @if (item.id !== 'home') {
              <a [routerLink]="item.route" class="rounded-2xl border border-border bg-card p-5 transition hover:border-primary hover:bg-accent">
                <p class="font-semibold text-accent-foreground">{{ item.label }}</p><p class="mt-1 text-xs text-muted-foreground">{{ t('home.openSection') }}</p>
              </a>
            }
          }
          <a routerLink="/dashboard/profile" class="rounded-2xl border border-border bg-card p-5 transition hover:border-primary hover:bg-accent"><p class="font-semibold text-accent-foreground">{{ t('nav.profile') }}</p><p class="mt-1 text-xs text-muted-foreground">{{ t('home.reviewProfile') }}</p></a>
        </section>
        @if (navigation.visibleItems().length <= 3) { <p class="rounded-2xl border border-border bg-muted p-4 text-sm text-muted-foreground">{{ t('home.noModules') }}</p> }
      } @else {
        <section class="rounded-3xl border border-border bg-card p-8 text-sm text-muted-foreground">{{ t('home.sessionError') }}</section>
      }
    </div>`
})
export class TenantHomePageComponent {
  readonly auth = inject(AuthFacade);
  readonly permissions = inject(PermissionService);
  readonly navigation = inject(TenantNavigationService);
  private readonly languageState = inject(TenantLanguageStateService);
  t(key: TenantTextKey): string { return this.languageState.t(key); }
}
