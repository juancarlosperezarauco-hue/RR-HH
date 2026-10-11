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
      <div class="rounded-3xl border border-border/30 bg-card p-8 text-center shadow-sm">
        @if (useCase.activationStatus()?.active) {
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-primary text-primary-foreground">
            <lucide-icon name="check-circle" class="h-8 w-8"></lucide-icon>
          </div>

          <h1 class="mt-6 text-3xl font-black text-foreground">
            Plan actualizado
          </h1>

          <p class="mt-4 text-sm leading-7 text-muted-foreground">
            Tu suscripción ya fue activada con el plan {{ useCase.activationStatus()?.planCode }}.
          </p>
        } @else if (useCase.activationStatus()?.failed) {
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-warning/10 text-warning">
            <lucide-icon name="circle-alert" class="h-8 w-8"></lucide-icon>
          </div>

          <h1 class="mt-6 text-3xl font-black text-foreground">
            Activación en revisión
          </h1>

          <p class="mt-4 text-sm leading-7 text-muted-foreground">
            No vuelvas a pagar. El pago fue recibido, pero necesitamos reconciliar la activación.
          </p>
        } @else {
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-accent text-primary">
            <lucide-icon name="loader-circle" class="h-8 w-8 animate-spin"></lucide-icon>
          </div>

          <h1 class="mt-6 text-3xl font-black text-foreground">
            Pago recibido
          </h1>

          <p class="mt-4 text-sm leading-7 text-muted-foreground">
            Estamos activando tu nuevo plan. Esto puede tardar unos segundos.
          </p>
        }

        @if (useCase.error()) {
          <div class="mt-6 rounded-2xl border border-warning/30 bg-warning/10 p-4 text-left text-sm text-warning">
            {{ useCase.error() }}
          </div>
        }

        <div class="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <button
            type="button"
            (click)="checkNow()"
            class="inline-flex h-11 items-center justify-center rounded-xl border border-border/40 bg-card px-5 text-sm font-black text-primary hover:bg-accent">
            Revisar estado
          </button>

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
