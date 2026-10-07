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
      <div class="rounded-3xl border bg-white p-8 text-center shadow-sm">
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-amber-100 text-amber-700">
          <lucide-icon name="circle-alert" class="h-8 w-8"></lucide-icon>
        </div>

        <h1 class="mt-6 text-3xl font-black text-slate-950">
          Upgrade cancelado
        </h1>

        <p class="mt-4 text-sm leading-7 text-slate-600">
          No se realizó ningún cargo. Puedes volver a elegir un plan cuando quieras.
        </p>

        <div class="mt-8">
          <a
            routerLink="/dashboard/subscription"
            class="inline-flex h-11 items-center justify-center rounded-xl bg-emerald-700 px-5 text-sm font-black text-white hover:bg-emerald-800">
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
