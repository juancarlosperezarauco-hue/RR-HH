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
      class="relative flex min-h-[650px] flex-col rounded-[30px] border bg-white p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]"
      [class.border-[#2E7D32]]="isRecommended()"
      [class.ring-2]="isRecommended()"
      [class.ring-[#A5D6A7]]="isRecommended()"
      [class.shadow-[0_30px_80px_rgba(46,125,50,0.20)]]="isRecommended()"
      [class.border-[#DDEED8]]="!isRecommended()">
      @if (isRecommended()) {
        <div class="absolute -top-4 left-1/2 -translate-x-1/2 rounded-full bg-[#2E7D32] px-6 py-2 text-xs font-black uppercase tracking-[0.18em] text-white shadow-sm">Recomendado</div>
      }

      <div>
        <span
          class="inline-flex rounded-full border px-3 py-1 text-xs font-black uppercase tracking-[0.18em]"
          [class.border-[#A5D6A7]]="isRecommended()"
          [class.bg-[#E8F5E9]]="isRecommended()"
          [class.text-[#1B5E20]]="isRecommended()"
          [class.border-[#D6E8D1]]="!isRecommended()"
          [class.text-[#6B7D6C]]="!isRecommended()">
          {{ badgeLabel() }}
        </span>

        <h2 class="mt-6 text-2xl font-black text-[#083B16]">
          {{ displayName() }}
        </h2>

        <p class="mt-4 min-h-[72px] text-sm leading-7 text-[#405447]">
          {{ plan.description || defaultDescription() }}
        </p>
      </div>

      <div class="mt-8 min-h-[96px]">
        @if (plan.contactSales) {
          <p class="text-5xl font-black tracking-tight text-[#083B16]">A medida</p>
        } @else {
          <p class="text-[clamp(2.25rem,3vw,3.25rem)] font-black leading-none tracking-tight text-[#083B16]">
            {{ priceLabel() }}
          </p>

          <p class="mt-3 text-sm font-bold text-[#6B7D6C]">
            {{ periodDescription() }}
          </p>
        }
      </div>

      <div class="mt-8">
        @if (isFree()) {
          <a
            routerLink="/onboarding"
            [queryParams]="{ plan: 'DEMO' }"
            class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-[#083B16] px-5 text-sm font-black text-white transition-colors hover:bg-[#102016]">
            Crear DEMO
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>
        } @else if (plan.contactSales) {
          <button
            type="button"
            class="inline-flex h-11 w-full items-center justify-center rounded-xl border border-[#C8E6C9] bg-white px-5 text-sm font-black text-[#1B5E20] transition-colors hover:bg-[#F7FBF3]">
            Contactar ventas
          </button>
        } @else {
          <a
            routerLink="/onboarding"
            [queryParams]="{ plan: plan.code, billing: billingInterval }"
            class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl px-5 text-sm font-black transition-colors"
            [class.bg-[#2E7D32]]="isRecommended()"
            [class.text-white]="isRecommended()"
            [class.hover:bg-[#256428]]="isRecommended()"
            [class.bg-[#083B16]]="!isRecommended()"
            [class.text-white]="!isRecommended()"
            [class.hover:bg-[#102016]]="!isRecommended()">
            Obtener {{ displayName() }}
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>
        }
      </div>

      <div class="mt-8 border-t border-[#DDEED8] pt-7">
        <p class="mb-5 text-sm font-black text-[#083B16]">
          {{ featureHeader() }}
        </p>

        <ul class="space-y-4">
          @for (feature of features(); track feature) {
            <li class="flex gap-3 text-sm leading-6 text-[#405447]">
              <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-[#2E7D32]"></lucide-icon>
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
