import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { PublicBillingPlan } from '../../../entities/billing';
import { BillingInterval } from '../pricing-page/pricing-page.component';

@Component({
  selector: 'app-pricing-plan-card',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <article
      class="relative flex min-h-[650px] flex-col rounded-[30px] border bg-card p-6 shadow-lg"
      [class.border-primary]="isRecommended()"
      [class.ring-2]="isRecommended()"
      [class.ring-secondary]="isRecommended()"
      [class.shadow-lg]="isRecommended()"
      [class.border-border]="!isRecommended()">
      @if (isRecommended()) {
        <div class="absolute -top-4 left-1/2 -translate-x-1/2 rounded-full bg-primary px-6 py-2 text-xs font-black uppercase tracking-[0.18em] text-primary-foreground shadow-sm">Recomendado</div>
      }

      <div>
        <span
          class="inline-flex rounded-full border px-3 py-1 text-xs font-black uppercase tracking-[0.18em]"
          [class.border-secondary]="isRecommended()"
          [class.bg-accent]="isRecommended()"
          [class.text-accent-foreground]="isRecommended()"
          [class.border-border]="!isRecommended()"
          [class.text-muted-foreground]="!isRecommended()">
          {{ badgeLabel() }}
        </span>

        <h2 class="mt-6 text-2xl font-black text-accent-foreground">
          {{ displayName() }}
        </h2>

        <p class="mt-4 min-h-[72px] text-sm leading-7 text-muted-foreground">
          {{ plan.description || defaultDescription() }}
        </p>
      </div>

      <div class="mt-8 min-h-[96px]">
        @if (plan.contactSales) {
          <p class="text-5xl font-black tracking-tight text-accent-foreground">A medida</p>
        } @else {
          <p class="text-[clamp(2.25rem,3vw,3.25rem)] font-black leading-none tracking-tight text-accent-foreground">
            {{ priceLabel() }}
          </p>

          <p class="mt-3 text-sm font-bold text-muted-foreground">
            {{ periodDescription() }}
          </p>
        }
      </div>

      <div class="mt-8">
        @if (isFree()) {
          <a
            routerLink="/onboarding"
            [queryParams]="{ plan: 'DEMO' }"
            class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-accent-foreground px-5 text-sm font-black text-primary-foreground transition-colors hover:bg-sidebar">
            Crear DEMO
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>
        } @else if (plan.contactSales) {
          <button
            type="button"
            class="inline-flex h-11 w-full items-center justify-center rounded-xl border border-border bg-card px-5 text-sm font-black text-accent-foreground transition-colors hover:bg-background">
            Contactar ventas
          </button>
        } @else {
          <a
            routerLink="/onboarding"
            [queryParams]="{ plan: plan.code, billing: billingInterval }"
            class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl px-5 text-sm font-black transition-colors"
            [class.bg-primary]="isRecommended()"
            [class.text-primary-foreground]="isRecommended()"
            [class.hover:bg-primary/90]="isRecommended()"
            [class.bg-accent-foreground]="!isRecommended()"
            [class.text-primary-foreground]="!isRecommended()"
            [class.hover:bg-sidebar]="!isRecommended()">
            Obtener {{ displayName() }}
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>
        }
      </div>

      <div class="mt-8 border-t border-border pt-7">
        <p class="mb-5 text-sm font-black text-accent-foreground">
          {{ featureHeader() }}
        </p>

        <ul class="space-y-4">
          @for (feature of features(); track feature) {
            <li class="flex gap-3 text-sm leading-6 text-muted-foreground">
              <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-primary"></lucide-icon>
              <span>{{ feature }}</span>
            </li>
          }
        </ul>
      </div>
    </article>
  `
})
export class PricingPlanCardComponent {
  @Input({ required: true }) plan!: PublicBillingPlan;
  @Input({ required: true }) billingInterval!: BillingInterval;

  isFree(): boolean {
    const code = this.plan.code?.toUpperCase();
    return code === 'DEMO' || code === 'FREE' || this.plan.planType === 'DEMO';
  }

  isRecommended(): boolean {
    const code = this.plan.code?.toUpperCase();
    return code === 'PRO' || code === 'PROFESSIONAL';
  }

  displayName(): string {
    const code = this.plan.code?.toUpperCase();

    if (code === 'DEMO' || code === 'FREE') return 'Free';
    if (code === 'PRO') return 'Professional';

    return this.plan.name;
  }

  badgeLabel(): string {
    if (this.isFree()) return 'Plan gratis';
    if (this.plan.contactSales) return 'Enterprise';
    return 'Paid';
  }

  priceLabel(): string {
    if (this.isFree()) return `${this.plan.currency || 'USD'} 0`;

    const amount = this.billingInterval === 'MONTHLY' ? this.plan.monthlyAmount : this.plan.yearlyAmount;

    if (amount == null) return 'A medida';

    return `${this.plan.currency || 'USD'} ${this.formatMoney(amount)}`;
  }

  periodDescription(): string {
    if (this.isFree()) return 'Demo inicial sin tarjeta';
    if (this.plan.contactSales) return '';

    return this.billingInterval === 'MONTHLY' ? 'Facturación mensual' : 'Facturación anual';
  }

  defaultDescription(): string {
    if (this.isFree()) return 'Prueba la plataforma sin costo antes de pasar a un plan pagado.';
    if (this.plan.contactSales) return 'Plan personalizado para empresas que necesitan condiciones comerciales especiales.';
    return 'Plan de suscripción para operar tu tenant con pago seguro mediante Stripe.';
  }

  featureHeader(): string {
    if (this.isFree()) return 'Incluye:';
    if (this.plan.contactSales) return 'Para empresas que necesitan:';
    return 'Todo lo necesario para operar:';
  }

  features(): string[] {
    if (this.isFree()) {
      return [
        `Hasta ${this.plan.maxUsers} usuarios`,
        `Hasta ${this.plan.maxRoles} roles personalizados`,
        `${this.plan.trialDays ?? 10} días de prueba`,
        'Sin tarjeta de crédito',
        'Ideal para validar el flujo completo'
      ];
    }

    if (this.plan.contactSales) {
      return ['Capacidad personalizada', 'Condiciones comerciales especiales', 'Acompañamiento comercial', 'No usa checkout automático'];
    }

    return [`Hasta ${this.plan.maxUsers} usuarios`, `Hasta ${this.plan.maxRoles} roles personalizados`, 'Pago seguro con Stripe', 'Puedes cambiar de plan desde tu sesión'];
  }

  private formatMoney(value: number): string {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: value % 1 === 0 ? 0 : 2,
      maximumFractionDigits: 2
    }).format(value);
  }
}
