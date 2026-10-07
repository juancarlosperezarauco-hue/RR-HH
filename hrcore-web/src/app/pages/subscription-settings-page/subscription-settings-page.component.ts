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
          <p class="text-sm font-black uppercase tracking-[0.2em] text-emerald-700">
            Suscripción
          </p>

          <h1 class="mt-2 text-3xl font-black text-slate-950">
            Plan y facturación
          </h1>

          <p class="mt-2 max-w-2xl text-sm leading-7 text-slate-600">
            Revisa tu plan actual y mejora tu suscripción cuando necesites más capacidad.
          </p>
        </div>

        <div class="inline-flex w-fit rounded-full border border-emerald-200 bg-white p-1 shadow-sm">
          <button
            type="button"
            (click)="billingInterval.set('MONTHLY')"
            class="h-10 rounded-full px-5 text-sm font-black"
            [class.bg-emerald-700]="billingInterval() === 'MONTHLY'"
            [class.text-white]="billingInterval() === 'MONTHLY'"
            [class.text-emerald-700]="billingInterval() !== 'MONTHLY'">
            Mensual
          </button>

          <button
            type="button"
            (click)="billingInterval.set('YEARLY')"
            class="h-10 rounded-full px-5 text-sm font-black"
            [class.bg-emerald-700]="billingInterval() === 'YEARLY'"
            [class.text-white]="billingInterval() === 'YEARLY'"
            [class.text-emerald-700]="billingInterval() !== 'YEARLY'">
            Anual
          </button>
        </div>
      </div>

      @if (useCase.error()) {
        <div class="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">
          {{ useCase.error() }}
        </div>
      }

      @if (useCase.loading()) {
        <div class="flex min-h-[300px] items-center justify-center rounded-3xl border bg-white">
          <div class="flex items-center gap-3 text-slate-600">
            <lucide-icon name="loader-circle" class="h-5 w-5 animate-spin"></lucide-icon>
            Cargando suscripción...
          </div>
        </div>
      } @else {
        @if (useCase.current()) {
          <article class="rounded-3xl border border-emerald-100 bg-white p-6 shadow-sm">
            <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <p class="text-xs font-black uppercase tracking-[0.18em] text-emerald-700">
                  Plan actual
                </p>

                <h2 class="mt-2 text-2xl font-black text-slate-950">
                  {{ useCase.current()?.planName }}
                </h2>

                <p class="mt-2 text-sm text-slate-600">
                  Estado: <strong>{{ useCase.current()?.status }}</strong>
                </p>
              </div>

              <div class="grid gap-3 sm:grid-cols-3">
                <div class="rounded-2xl bg-emerald-50 p-4">
                  <p class="text-xs font-bold text-slate-500">Usuarios</p>
                  <p class="mt-1 text-xl font-black text-slate-950">{{ currentMaxUsers() }}</p>
                </div>

                <div class="rounded-2xl bg-emerald-50 p-4">
                  <p class="text-xs font-bold text-slate-500">Roles</p>
                  <p class="mt-1 text-xl font-black text-slate-950">{{ currentMaxRoles() }}</p>
                </div>

                <div class="rounded-2xl bg-emerald-50 p-4">
                  <p class="text-xs font-bold text-slate-500">Trial</p>
                  <p class="mt-1 text-xl font-black text-slate-950">
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
              class="flex min-h-[420px] flex-col rounded-3xl border bg-white p-6 shadow-sm"
              [class.border-emerald-500]="plan.currentPlan"
              [class.ring-2]="plan.currentPlan"
              [class.ring-emerald-100]="plan.currentPlan"
              [class.border-slate-200]="!plan.currentPlan">

              <div>
                <span class="inline-flex rounded-full bg-emerald-50 px-3 py-1 text-xs font-black uppercase tracking-[0.16em] text-emerald-700">
                  {{ plan.currentPlan ? 'Actual' : plan.planType }}
                </span>

                <h3 class="mt-5 text-2xl font-black text-slate-950">
                  {{ plan.name }}
                </h3>

                <p class="mt-3 min-h-[70px] text-sm leading-7 text-slate-600">
                  {{ plan.description || 'Plan disponible para tu tenant.' }}
                </p>

                <div class="mt-7">
                  <p class="text-4xl font-black text-slate-950">
                    {{ priceLabel(plan) }}
                  </p>
                  <p class="mt-2 text-sm font-bold text-slate-500">
                    {{ billingInterval() === 'MONTHLY' ? 'Facturación mensual' : 'Facturación anual' }}
                  </p>
                </div>

                <ul class="mt-7 space-y-3 text-sm text-slate-600">
                  <li class="flex gap-3">
                    <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-emerald-700"></lucide-icon>
                    Hasta {{ displayMaxUsers(plan) }} usuarios
                  </li>
                  <li class="flex gap-3">
                    <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-emerald-700"></lucide-icon>
                    Hasta {{ displayMaxRoles(plan) }} roles
                  </li>
                  <li class="flex gap-3">
                    <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-emerald-700"></lucide-icon>
                    Pago seguro con Stripe
                  </li>
                </ul>
              </div>

              <div class="mt-auto pt-7">
                @if (isCurrentPlan(plan)) {
                  <button
                    type="button"
                    disabled
                    class="h-11 w-full rounded-xl bg-slate-100 text-sm font-black text-slate-400">
                    Plan actual
                  </button>
                } @else if (plan.contactSales) {
                  <button
                    type="button"
                    disabled
                    class="h-11 w-full rounded-xl border border-slate-200 bg-white text-sm font-black text-slate-500">
                    Contactar ventas
                  </button>
                } @else if (!canUpgrade(plan)) {
                  <button
                    type="button"
                    disabled
                    class="h-11 w-full rounded-xl bg-slate-100 text-sm font-black text-slate-400">
                    No disponible
                  </button>
                } @else {
                  <button
                    type="button"
                    (click)="upgrade(plan)"
                    [disabled]="useCase.checkoutLoading()"
                    class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-5 text-sm font-black text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60">
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

    return this.resolvePlanLimit(current.planCode, current.maxUsers, 'users');
  }

  currentMaxRoles(): number {
    const current = this.useCase.current();
    if (!current) {
      return 0;
    }

    return this.resolvePlanLimit(current.planCode, current.maxRoles, 'roles');
  }

  canUpgrade(plan: TenantBillingPlan): boolean {
    if (this.isCurrentPlan(plan)) {
      return false;
    }

    if (plan.contactSales) {
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

    if (plan.contactSales || amount == null) {
      return 'A medida';
    }

    return `${plan.currency || 'USD'} ${amount}`;
  }

  displayMaxUsers(plan: TenantBillingPlan): number {
    return this.resolvePlanLimit(plan.code, plan.maxUsers, 'users');
  }

  displayMaxRoles(plan: TenantBillingPlan): number {
    return this.resolvePlanLimit(plan.code, plan.maxRoles, 'roles');
  }

  upgrade(plan: TenantBillingPlan): void {
    if (!plan.availableForCheckout) {
      return;
    }

    void this.useCase.createCheckout(plan.code, this.billingInterval());
  }

  private resolvePlanLimit(code: string | null | undefined, value: number | null | undefined, kind: 'users' | 'roles'): number {
    const normalized = (code || '').trim().toUpperCase();
    const direct = typeof value === 'number' && value > 0 ? value : null;

    if (direct !== null) {
      return direct;
    }

    const fallback = this.planLimitFallbacks()[normalized];
    if (fallback) {
      return kind === 'users' ? fallback.users : fallback.roles;
    }

    return 0;
  }

  private planLimitFallbacks(): Record<string, { users: number; roles: number }> {
    return {
      DEMO: { users: 5, roles: 3 },
      BASIC: { users: 10, roles: 5 },
      PRO: { users: 25, roles: 10 },
      ENTERPRISE: { users: 9999, roles: 999 }
    };
  }
}
