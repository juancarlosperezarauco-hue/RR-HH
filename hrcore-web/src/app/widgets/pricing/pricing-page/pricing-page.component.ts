import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { PublicPlansUseCase } from '../../../features/billing';
import { PublicBillingPlan } from '../../../entities/billing';
import { PricingCtaComponent } from '../pricing-cta/pricing-cta.component';
import { PricingFaqComponent } from '../pricing-faq/pricing-faq.component';
import { PricingGridComponent } from '../pricing-grid/pricing-grid.component';
import { PricingHeroComponent } from '../pricing-hero/pricing-hero.component';

export type BillingInterval = 'MONTHLY' | 'YEARLY';

@Component({
  selector: 'app-pricing-page-widget',
  standalone: true,
  imports: [CommonModule, PricingHeroComponent, PricingGridComponent, PricingFaqComponent, PricingCtaComponent],
  template: `
    <main class="min-h-screen bg-[#F7FBF3] text-[#101827]">
      <app-pricing-hero
        [billingInterval]="billingInterval()"
        (billingIntervalChange)="billingInterval.set($event)" />

      <app-pricing-grid
        [plans]="plans()"
        [status]="plansUseCase.status()"
        [error]="plansUseCase.error()"
        [billingInterval]="billingInterval()"
        (retry)="plansUseCase.loadPlans()" />

      <app-pricing-faq />

      <app-pricing-cta />
    </main>
  `
})
export class PricingPageWidgetComponent implements OnInit {
  readonly plansUseCase = inject(PublicPlansUseCase);

  readonly billingInterval = signal<BillingInterval>('MONTHLY');

  readonly plans = computed(() => {
    const backendPlans = this.plansUseCase.plans();
    const hasFree = backendPlans.some((plan) => {
      const code = plan.code?.toUpperCase();
      return code === 'DEMO' || code === 'FREE';
    });

    if (hasFree) {
      return backendPlans;
    }

    const freePlan: PublicBillingPlan = {
      id: 'free-plan-local',
      code: 'DEMO',
      name: 'Free',
      description: 'Prueba la plataforma sin costo antes de pasar a un plan pagado.',
      planType: 'DEMO',
      maxUsers: 3,
      maxRoles: 3,
      trialDays: 10,
      monthlyAmount: 0,
      yearlyAmount: 0,
      currency: 'USD',
      publicVisible: true,
      sortOrder: 0,
      active: true,
      currentPlan: false,
      availableForCheckout: false,
      contactSales: false
    };

    return [freePlan, ...backendPlans];
  });

  ngOnInit(): void {
    this.plansUseCase.loadPlans();
  }
}
