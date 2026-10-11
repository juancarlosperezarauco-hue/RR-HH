import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { PublicBillingPlan } from '../../../entities/billing';
import { BillingInterval } from '../pricing-page/pricing-page.component';
import { PricingPlanCardComponent } from '../pricing-plan-card/pricing-plan-card.component';

@Component({
  selector: 'app-pricing-grid',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PricingPlanCardComponent],
  template: `
    <section class="px-4 pb-20 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-7xl">
        @if (status === 'loading') {
          <div class="flex min-h-[360px] items-center justify-center rounded-[34px] border border-border bg-card shadow-sm">
            <div class="flex items-center gap-3 text-muted-foreground">
              <lucide-icon name="loader-circle" class="h-5 w-5 animate-spin"></lucide-icon>
              Cargando planes...
            </div>
          </div>
        } @else if (status === 'error') {
          <div class="rounded-[34px] border border-destructive/20 bg-destructive/5 p-7">
            <div class="flex items-start gap-3">
              <lucide-icon name="triangle-alert" class="mt-0.5 h-5 w-5 text-destructive"></lucide-icon>

              <div>
                <h2 class="font-black text-destructive">No se pudieron cargar los planes</h2>
                <p class="mt-1 text-sm text-destructive">{{ error }}</p>

                <button
                  type="button"
                  (click)="retry.emit()"
                  class="mt-5 inline-flex h-10 items-center justify-center rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground">
                  Reintentar
                </button>
              </div>
            </div>
          </div>
        } @else {
          <div class="grid gap-6 lg:grid-cols-4">
            @for (plan of orderedPlans(); track plan.id) {
              <app-pricing-plan-card [plan]="plan" [billingInterval]="billingInterval" />
            }
          </div>
        }
      </div>
    </section>
  `
})
export class PricingGridComponent {
  @Input({ required: true }) plans: PublicBillingPlan[] = [];
  @Input({ required: true }) status!: 'idle' | 'loading' | 'success' | 'error';
  @Input() error: string | null = null;
  @Input({ required: true }) billingInterval!: BillingInterval;

  @Output() retry = new EventEmitter<void>();

  orderedPlans(): PublicBillingPlan[] {
    return [...this.plans].sort((a, b) => this.planOrder(a) - this.planOrder(b));
  }

  private planOrder(plan: PublicBillingPlan): number {
    const code = plan.code?.toUpperCase();

    if (code === 'DEMO' || code === 'FREE') return 0;
    if (code === 'BASIC') return 1;
    if (code === 'PRO' || code === 'PROFESSIONAL') return 2;
    if (code === 'ENTERPRISE') return 3;

    return plan.sortOrder ?? 99;
  }
}
