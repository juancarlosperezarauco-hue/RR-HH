import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { TenantSubscriptionUseCase } from '../../features/billing';

@Component({
  selector: 'app-subscription-upgrade-success-page',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section class="mx-auto max-w-3xl px-4 py-12">
      <div class="rounded-3xl border border-emerald-100 bg-white p-8 text-center shadow-sm">
        @if (useCase.activationStatus()?.active) {
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-emerald-700 text-white">
            <lucide-icon name="check-circle" class="h-8 w-8"></lucide-icon>
          </div>

          <h1 class="mt-6 text-3xl font-black text-slate-950">
            Plan actualizado
          </h1>

          <p class="mt-4 text-sm leading-7 text-slate-600">
            Tu suscripción ya fue activada con el plan {{ useCase.activationStatus()?.planCode }}.
          </p>
        } @else if (useCase.activationStatus()?.failed) {
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-amber-100 text-amber-700">
            <lucide-icon name="circle-alert" class="h-8 w-8"></lucide-icon>
          </div>

          <h1 class="mt-6 text-3xl font-black text-slate-950">
            Activación en revisión
          </h1>

          <p class="mt-4 text-sm leading-7 text-slate-600">
            No vuelvas a pagar. El pago fue recibido, pero necesitamos reconciliar la activación.
          </p>
        } @else {
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-emerald-50 text-emerald-700">
            <lucide-icon name="loader-circle" class="h-8 w-8 animate-spin"></lucide-icon>
          </div>

          <h1 class="mt-6 text-3xl font-black text-slate-950">
            Pago recibido
          </h1>

          <p class="mt-4 text-sm leading-7 text-slate-600">
            Estamos activando tu nuevo plan. Esto puede tardar unos segundos.
          </p>
        }

        @if (useCase.error()) {
          <div class="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-left text-sm text-amber-800">
            {{ useCase.error() }}
          </div>
        }

        <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <button
            type="button"
            (click)="checkNow()"
            class="inline-flex h-11 items-center justify-center rounded-xl border border-emerald-200 bg-white px-5 text-sm font-black text-emerald-700 hover:bg-emerald-50">
            Revisar estado
          </button>

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
export class SubscriptionUpgradeSuccessPageComponent implements OnInit, OnDestroy {
  readonly useCase = inject(TenantSubscriptionUseCase);
  private readonly route = inject(ActivatedRoute);

  private pollingId: ReturnType<typeof setInterval> | null = null;
  private stripeSessionId: string | null = null;
  private attempts = 0;

  ngOnInit(): void {
    const pending = this.useCase.pendingCheckout();
    const sessionFromUrl = this.route.snapshot.queryParamMap.get('session_id');

    this.stripeSessionId = sessionFromUrl || pending?.stripeSessionId || null;

    if (this.stripeSessionId) {
      this.checkNow();
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

    void this.useCase.checkActivationStatus(this.stripeSessionId);
  }

  private startPolling(): void {
    this.stopPolling();

    this.pollingId = setInterval(() => {
      const activation = this.useCase.activationStatus();

      if (activation?.active || activation?.failed) {
        this.stopPolling();
        return;
      }

      this.attempts++;

      if (this.attempts >= 15) {
        this.stopPolling();
        return;
      }

      this.checkNow();
    }, 3000);
  }

  private stopPolling(): void {
    if (this.pollingId) {
      clearInterval(this.pollingId);
      this.pollingId = null;
    }
  }
}
