import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TenantLanguageStateService } from '../../shared/lib/i18n/tenant-language-state.service';
import { TenantTextKey } from '../../shared/lib/i18n/keys';

@Component({
  selector: 'app-tenant-access-denied-page', standalone: true, imports: [RouterModule],
  template: `<div class="mx-auto max-w-xl rounded-3xl border border-border bg-card p-8 text-center shadow-sm"><h1 class="text-2xl font-black text-accent-foreground">{{ t('accessDenied.title') }}</h1><p class="mt-3 text-sm text-muted-foreground">{{ t('accessDenied.description') }}</p><div class="mt-6 flex justify-center gap-3"><a routerLink="/dashboard/home" class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">{{ t('accessDenied.home') }}</a><a routerLink="/dashboard/profile" class="rounded-full border border-border px-4 py-2 text-sm font-semibold text-primary">{{ t('accessDenied.profile') }}</a></div></div>`
})
export class TenantAccessDeniedPageComponent {
  private readonly languageState = inject(TenantLanguageStateService);
  t(key: TenantTextKey): string { return this.languageState.t(key); }
}
