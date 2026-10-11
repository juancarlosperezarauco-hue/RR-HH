import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { CheckoutActivationStatusUseCase } from '../../features/billing';
import { TenantUpgradeCheckoutStorageService } from '../../entities/billing';
import { AuthStorageService } from '../../shared/lib/storage/auth-storage.service';
import { PublicFooterComponent, PublicNavbarComponent } from '../../shared/ui/public-layout';

@Component({
  selector: 'app-billing-success-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    PublicNavbarComponent,
    PublicFooterComponent
  ],
  template: `
    <div class="min-h-screen bg-background">
      <app-public-navbar />

      <main class="px-4 py-20 sm:px-6 lg:px-8">
        <section class="mx-auto max-w-3xl rounded-[34px] border border-border bg-card p-6 shadow-lg sm:p-8">
          <div class="rounded-[30px] border border-border bg-muted p-5 sm:p-7">
            @if (statusUseCase.status() === 'active') {
              <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-primary text-primary-foreground shadow-sm">
                <lucide-icon name="check-circle" class="h-8 w-8"></lucide-icon>
              </div>

              <h1 class="mt-6 text-center text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Tu compra fue confirmada
              </h1>

              <p class="mt-4 text-center text-sm leading-7 text-muted-foreground sm:text-base">
                Revisa tu correo y luego entra al login para empezar a usar el plan {{ planCode() }}.
              </p>
            } @else if (statusUseCase.status() === 'failed') {
              <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-muted text-foreground shadow-sm">
                <lucide-icon name="circle-alert" class="h-8 w-8"></lucide-icon>
              </div>

              <h1 class="mt-6 text-center text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Estamos terminando de activar tu suscripción
              </h1>

              <p class="mt-4 text-center text-sm leading-7 text-muted-foreground sm:text-base">
                No vuelvas a pagar. Tu compra ya fue registrada. Revisa tu correo y vuelve a intentar en unos segundos.
              </p>
            } @else {
              <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-accent text-primary shadow-sm">
                <lucide-icon name="loader-circle" class="h-8 w-8 animate-spin"></lucide-icon>
              </div>

              <h1 class="mt-6 text-center text-3xl font-black tracking-tight text-accent-foreground sm:text-4xl">
                Estamos revisando tu compra
              </h1>

              <p class="mt-4 text-center text-sm leading-7 text-muted-foreground sm:text-base">
                Tu pago ya llegó. Revisa tu correo y en breve quedará listo tu acceso al plan {{ planCode() }}.
              </p>
            }

            @if (pendingCheckout()) {
              <div class="mt-8 rounded-3xl border border-border bg-card p-5 text-left shadow-sm">
                <p class="text-xs font-black uppercase tracking-[0.18em] text-primary">
                  Resumen
                </p>

                <div class="mt-4 grid gap-3 text-sm sm:grid-cols-2">
                  <div class="rounded-2xl bg-muted px-4 py-3">
                    <p class="text-xs font-bold text-muted-foreground">Plan</p>
                    <p class="mt-1 font-black text-accent-foreground">{{ planCode() }}</p>
                  </div>

                  <div class="rounded-2xl bg-muted px-4 py-3">
                    <p class="text-xs font-bold text-muted-foreground">Facturación</p>
                    <p class="mt-1 font-black text-accent-foreground">{{ billingInterval() }}</p>
                  </div>

                  <div class="rounded-2xl bg-muted px-4 py-3 sm:col-span-2">
                    <p class="text-xs font-bold text-muted-foreground">Empresa</p>
                    <p class="mt-1 font-black text-accent-foreground">{{ pendingCheckout()?.companyName }}</p>
                  </div>
                </div>
              </div>
            }

            @if (statusUseCase.status() === 'error') {
              <div class="mt-6 rounded-2xl border border-border bg-muted p-4 text-left text-sm leading-7 text-foreground">
                No pudimos revisar el estado en este momento. No vuelvas a pagar. Puedes intentar nuevamente o entrar al login en unos segundos.
              </div>
            }

            <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
              @if (statusUseCase.status() !== 'active') {
                <button
                  type="button"
                  (click)="checkNow()"
                  class="inline-flex h-11 items-center justify-center rounded-xl border border-border bg-card px-5 text-sm font-black text-accent-foreground hover:bg-background">
                  Revisar estado
                </button>
              }

              <a
                routerLink="/login"
                [queryParams]="pendingCheckout()?.tenantSlug ? { tenant: pendingCheckout()?.tenantSlug, checkout: 'success' } : { checkout: 'success' }"
                class="inline-flex h-11 items-center justify-center rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground hover:bg-primary/90">
                Ir al login
              </a>
            </div>

            <p class="mt-6 text-center text-xs leading-6 text-muted-foreground">
              Si todavía no ves tu plan activo, espera unos segundos y vuelve a revisar.
            </p>
          </div>
        </section>
      </main>

      <app-public-footer />
    </div>
  `
})
export class BillingSuccessPageComponent implements OnInit, OnDestroy {
  private static readonly POLLING_INTERVAL_MS = 5000;
  private static readonly MAX_POLLING_ATTEMPTS = 60;

  readonly statusUseCase = inject(CheckoutActivationStatusUseCase);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tenantUpgradeCheckoutStorage = inject(TenantUpgradeCheckoutStorageService);
  private readonly authStorage = inject(AuthStorageService);

  private pollingId: ReturnType<typeof setInterval> | null = null;
  stripeSessionId: string | null = null;

  readonly pendingCheckout = computed(() => this.statusUseCase.pendingCheckout());

  readonly planCode = computed(() => {
    return this.statusUseCase.data()?.planCode
      || this.pendingCheckout()?.selectedPlanCode
      || this.pendingCheckout()?.requestedPlanCode
      || 'Plan';
  });

  readonly billingInterval = computed(() => {
    return this.statusUseCase.data()?.billingInterval
      || this.pendingCheckout()?.billingInterval
      || 'MONTHLY';
  });

  ngOnInit(): void {
    this.statusUseCase.reset();

    const tenantUpgradePending = this.tenantUpgradeCheckoutStorage.get();
    if (tenantUpgradePending && this.authStorage.hasValidTenantSession()) {
      void this.router.navigate(['/dashboard/subscription/success'], {
        queryParams: { session_id: tenantUpgradePending.stripeSessionId },
        replaceUrl: true
      });
      return;
    }

    const pending = this.statusUseCase.loadPendingCheckout();
    const sessionFromUrl = this.route.snapshot.queryParamMap.get('session_id');

    this.stripeSessionId = sessionFromUrl || pending?.checkoutSessionId || null;

    if (this.stripeSessionId) {
      void this.statusUseCase.checkStatus(this.stripeSessionId);
      this.startPolling();
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  checkNow(): void {
    if (!this.stripeSessionId) {
      return;
    }

    void this.statusUseCase.checkStatus(this.stripeSessionId);
  }

  private startPolling(): void {
    this.stopPolling();

    this.pollingId = setInterval(() => {
      const status = this.statusUseCase.status();

      if (status === 'active' || status === 'failed') {
        this.stopPolling();
        return;
      }

      if (this.statusUseCase.attempts() >= BillingSuccessPageComponent.MAX_POLLING_ATTEMPTS) {
        this.stopPolling();
        return;
      }

      if (this.stripeSessionId) {
        void this.statusUseCase.checkStatus(this.stripeSessionId);
      }
    }, BillingSuccessPageComponent.POLLING_INTERVAL_MS);
  }

  private stopPolling(): void {
    if (this.pollingId) {
      clearInterval(this.pollingId);
      this.pollingId = null;
    }
  }
}
