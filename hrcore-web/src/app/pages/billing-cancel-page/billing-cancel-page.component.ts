import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { PendingCheckoutStorageService } from '../../entities/billing';
import { TenantUpgradeCheckoutStorageService } from '../../entities/billing';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { PublicFooterComponent, PublicNavbarComponent } from '../../shared/ui/public-layout';

@Component({
  selector: 'app-billing-cancel-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    PublicNavbarComponent,
    PublicFooterComponent
  ],
  template: `
    <div class="min-h-screen bg-[#F7FBF3]">
      <app-public-navbar />

      <main class="px-4 py-20 sm:px-6 lg:px-8">
        <section class="mx-auto max-w-3xl rounded-[34px] border border-[#DDEED8] bg-white p-8 text-center shadow-[0_24px_70px_rgba(27,94,32,0.12)]">
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-[#FBF4EA] text-[#A87824]">
            <lucide-icon name="circle-alert" class="h-8 w-8"></lucide-icon>
          </div>

          <h1 class="mt-6 text-4xl font-black text-[#083B16]">
            Pago cancelado
          </h1>

          <p class="mt-4 text-sm leading-7 text-[#405447]">
            No se completó el pago. Puedes volver a elegir un plan o crear una demo gratuita.
          </p>

          <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <a
              routerLink="/prices"
              class="inline-flex h-11 items-center justify-center rounded-xl bg-[#2E7D32] px-5 text-sm font-black text-white hover:bg-[#256428]">
              Volver a precios
            </a>

            <a
              routerLink="/onboarding"
              [queryParams]="{ plan: 'DEMO' }"
              class="inline-flex h-11 items-center justify-center rounded-xl border border-[#C8E6C9] bg-white px-5 text-sm font-black text-[#1B5E20] hover:bg-[#F7FBF3]">
              Crear demo
            </a>
          </div>
        </section>
      </main>

      <app-public-footer />
    </div>
  `
})
export class BillingCancelPageComponent {
  private readonly storage = inject(PendingCheckoutStorageService);
  private readonly tenantUpgradeCheckoutStorage = inject(TenantUpgradeCheckoutStorageService);
  private readonly authStorage = inject(AuthStorageService);
  private readonly router = inject(Router);

  constructor() {
    this.storage.clear();
    const tenantUpgradePending = this.tenantUpgradeCheckoutStorage.get();
    if (tenantUpgradePending && this.authStorage.hasValidTenantSession()) {
      void this.router.navigate(['/dashboard/subscription/cancel'], { replaceUrl: true });
      return;
    }
  }
}
