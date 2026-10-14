import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { BillingInterval, TenantBillingPlan } from '../../entities/billing';
import { TenantSubscriptionUseCase } from '../../features/billing';

@Component({
  selector: 'app-subscription-settings-page',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section class="space-y-8">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-sm font-black uppercase tracking-[0.2em] text-primary">
            Suscripción
          </p>

          <h1 class="mt-2 text-3xl font-black text-foreground">
            Plan y facturación
          </h1>

          <p class="mt-2 max-w-2xl text-sm leading-7 text-muted-foreground">
            Revisa tu plan actual y mejora tu suscripción cuando necesites más capacidad.
          </p>
        </div>

        <div class="inline-flex w-fit rounded-full border border-border/40 bg-card p-1 shadow-sm">
          <button
            type="button"
            (click)="billingInterval.set('MONTHLY')"
            class="h-10 rounded-full px-5 text-sm font-black"
            [class.bg-primary]="billingInterval() === 'MONTHLY'"
            [class.text-primary-foreground]="billingInterval() === 'MONTHLY'"
            [class.text-primary]="billingInterval() !== 'MONTHLY'">
            Mensual
          </button>

          <button
            type="button"
            (click)="billingInterval.set('YEARLY')"
            class="h-10 rounded-full px-5 text-sm font-black"
            [class.bg-primary]="billingInterval() === 'YEARLY'"
            [class.text-primary-foreground]="billingInterval() === 'YEARLY'"
            [class.text-primary]="billingInterval() !== 'YEARLY'">
            Anual
          </button>
        </div>
      </div>

      @if (useCase.error()) {
        <div class="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm font-semibold text-destructive">
          {{ useCase.error() }}
        </div>
      }

      @if (useCase.loading()) {
        <div class="flex min-h-[300px] items-center justify-center rounded-3xl border bg-card">
          <div class="flex items-center gap-3 text-muted-foreground">
            <lucide-icon name="loader-circle" class="h-5 w-5 animate-spin"></lucide-icon>
            Cargando suscripción...
          </div>
        </div>
      } @else {
        @if (useCase.current()) {
          <article class="rounded-3xl border border-border/30 bg-card p-6 shadow-sm">
            <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <p class="text-xs font-black uppercase tracking-[0.18em] text-primary">
                  Plan actual
                </p>

                <h2 class="mt-2 text-2xl font-black text-foreground">
                  {{ useCase.current()?.planName }}
                </h2>

                <p class="mt-2 text-sm text-muted-foreground">
                  Estado: <strong>{{ useCase.current()?.status }}</strong>
                </p>
              </div>

              <div class="grid gap-3 sm:grid-cols-3">
                <div class="rounded-2xl bg-accent p-4">
                  <p class="text-xs font-bold text-muted-foreground">Usuarios</p>
                  <p class="mt-1 text-xl font-black text-foreground">{{ currentMaxUsers() }}</p>
                </div>

                <div class="rounded-2xl bg-accent p-4">
                  <p class="text-xs font-bold text-muted-foreground">Roles</p>
                  <p class="mt-1 text-xl font-black text-foreground">{{ currentMaxRoles() }}</p>
                </div>

                <div class="rounded-2xl bg-accent p-4">
                  <p class="text-xs font-bold text-muted-foreground">Trial</p>
                  <p class="mt-1 text-xl font-black text-foreground">
                    {{ useCase.current()?.trial ? 'Sí' : 'No' }}
                  </p>
                </div>
              </div>
            </div>
          </article>
        }

        <div class="grid gap-6 xl:grid-cols-3">
          @for (plan of upgradePlans(); track plan.id) {
            <article
              class="flex min-h-[420px] flex-col rounded-3xl border bg-card p-6 shadow-sm"
              [class.border-accent0]="plan.currentPlan"
              [class.ring-2]="plan.currentPlan"
              [class.ring-border/30]="plan.currentPlan"
              [class.border-border]="!plan.currentPlan">

              <div>
                <span class="inline-flex rounded-full bg-accent px-3 py-1 text-xs font-black uppercase tracking-[0.16em] text-primary">
                  {{ plan.currentPlan ? 'Actual' : plan.planType }}
                </span>

                <h3 class="mt-5 text-2xl font-black text-foreground">
                  {{ plan.name }}
                </h3>

                <p class="mt-3 min-h-[70px] text-sm leading-7 text-muted-foreground">
                  {{ plan.description || 'Plan disponible para tu tenant.' }}
                </p>

                <div class="mt-7">
                  <p class="text-4xl font-black text-foreground">
                    {{ priceLabel(plan) }}
                  </p>
                  <p class="mt-2 text-sm font-bold text-muted-foreground">
                    {{ billingInterval() === 'MONTHLY' ? 'Facturación mensual' : 'Facturación anual' }}
                  </p>
                </div>

                <ul class="mt-7 space-y-3 text-sm text-muted-foreground">
                  <li class="flex gap-3">
                    <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-primary"></lucide-icon>
                    Gestión de usuarios según el plan contratado
                  </li>
                  <li class="flex gap-3">
                    <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-primary"></lucide-icon>
                    Roles y permisos para tu equipo
                  </li>
                  <li class="flex gap-3">
                    <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-primary"></lucide-icon>
                    Pago seguro con Stripe
                  </li>
                </ul>
              </div>

              <div class="mt-auto pt-7">
                @if (isCurrentPlan(plan)) {
                  <button
                    type="button"
                    disabled
                    class="h-11 w-full rounded-xl bg-muted text-sm font-black text-muted-foreground">
                    Plan actual
                  </button>
                } @else if (!canUpgrade(plan)) {
                  <button
                    type="button"
                    disabled
                    class="h-11 w-full rounded-xl bg-muted text-sm font-black text-muted-foreground">
                    No disponible
                  </button>
                } @else {
                  <button
                    type="button"
                    (click)="upgrade(plan)"
                    [disabled]="useCase.checkoutLoading()"
                    class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60">
                    @if (useCase.checkoutLoading()) {
                      <lucide-icon name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
                    } @else {
                      <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
                    }
                    Mejorar plan
                  </button>
                }
              </div>
            </article>
          }
        </div>
      }
    </section>
  `
})
export class SubscriptionSettingsPageComponent implements OnInit {
  readonly useCase = inject(TenantSubscriptionUseCase);
  readonly billingInterval = signal<BillingInterval>('MONTHLY');

  readonly upgradePlans = computed(() => {
    return this.useCase.plans()
      .filter((plan) => plan.planType !== 'DEMO')
      .sort((a, b) => a.sortOrder - b.sortOrder);
  });

  ngOnInit(): void {
    void this.useCase.load();
  }

  currentPlanCode(): string {
    return (this.useCase.current()?.planCode || this.useCase.current()?.planName || '')
      .trim()
      .toUpperCase();
  }

  isCurrentPlan(plan: TenantBillingPlan): boolean {
    return plan.code.trim().toUpperCase() === this.currentPlanCode();
  }

  currentMaxUsers(): number {
    const current = this.useCase.current();
    if (!current) {
      return 0;
    }

    return current.maxUsers;
  }

  currentMaxRoles(): number {
    const current = this.useCase.current();
    if (!current) {
      return 0;
    }

    return current.maxRoles;
  }

  canUpgrade(plan: TenantBillingPlan): boolean {
    if (this.isCurrentPlan(plan)) {
      return false;
    }

    if (plan.planType === 'DEMO') {
      return false;
    }

    const currentSort = this.useCase.plans()
      .find((item) => item.code.trim().toUpperCase() === this.currentPlanCode())
      ?.sortOrder ?? 0;

    return plan.availableForCheckout && plan.sortOrder > currentSort;
  }

  priceLabel(plan: TenantBillingPlan): string {
    const amount = this.billingInterval() === 'MONTHLY'
      ? plan.monthlyAmount
      : plan.yearlyAmount;

    if (amount == null) {
      return 'A medida';
    }

    return `${plan.currency || 'USD'} ${amount}`;
  }

  upgrade(plan: TenantBillingPlan): void {
    if (!plan.availableForCheckout) {
      return;
    }

    void this.useCase.createCheckout(plan.code, this.billingInterval());
  }

}
