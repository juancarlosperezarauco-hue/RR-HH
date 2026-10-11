import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { TenantUpgradeCheckoutStorageService } from '../../entities/billing';

@Component({
  selector: 'app-subscription-upgrade-cancel-page',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section class="mx-auto max-w-3xl px-4 py-12">
      <div class="rounded-3xl border bg-card p-8 text-center shadow-sm">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-warning/10 text-warning">
          <lucide-icon name="circle-alert" class="h-8 w-8"></lucide-icon>
        </div>

        <h1 class="mt-6 text-3xl font-black text-foreground">
          Upgrade cancelado
        </h1>

        <p class="mt-4 text-sm leading-7 text-muted-foreground">
          No se realizó ningún cargo. Puedes volver a elegir un plan cuando quieras.
        </p>

        <div class="mt-8">
          <a
            routerLink="/dashboard/subscription"
            class="inline-flex h-11 items-center justify-center rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground hover:bg-primary/90">
            Volver a suscripción
          </a>
        </div>
      </div>
    </section>
  `
})
export class SubscriptionUpgradeCancelPageComponent {
  private readonly storage = inject(TenantUpgradeCheckoutStorageService);

  constructor() {
    this.storage.clear();
  }
}
