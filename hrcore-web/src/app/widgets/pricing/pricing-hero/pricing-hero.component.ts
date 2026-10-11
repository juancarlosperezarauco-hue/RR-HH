import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { BillingInterval } from '../pricing-page/pricing-page.component';

@Component({
  selector: 'app-pricing-hero',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section class="relative isolate overflow-hidden px-4 pb-10 pt-16 sm:px-6 lg:px-8 lg:pb-12 lg:pt-20">
      <div class="absolute inset-0 -z-10 bg-accent"></div>

      <div class="mx-auto max-w-7xl">
        <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-3xl">
            <p class="text-sm font-black uppercase tracking-[0.24em] text-primary">Precios</p>

            <h1 class="mt-4 text-4xl font-black leading-tight tracking-tight text-accent-foreground sm:text-5xl lg:text-6xl">Elige el plan para crear tu tenant.</h1>

            <p class="mt-5 max-w-2xl text-base leading-8 text-muted-foreground">Inicia gratis con DEMO o crea tu organización con una suscripción pagada usando Stripe desde el registro.</p>
          </div>

          <div class="inline-flex w-fit rounded-full border border-border bg-card p-1 shadow-sm">
            <button
              type="button"
              (click)="billingIntervalChange.emit('MONTHLY')"
              class="h-10 rounded-full px-6 text-sm font-black transition-colors"
              [class.bg-primary]="billingInterval === 'MONTHLY'"
              [class.text-primary-foreground]="billingInterval === 'MONTHLY'"
              [class.text-primary]="billingInterval !== 'MONTHLY'">
              Mensual
            </button>

            <button
              type="button"
              (click)="billingIntervalChange.emit('YEARLY')"
              class="h-10 rounded-full px-6 text-sm font-black transition-colors"
              [class.bg-primary]="billingInterval === 'YEARLY'"
              [class.text-primary-foreground]="billingInterval === 'YEARLY'"
              [class.text-primary]="billingInterval !== 'YEARLY'">
              Anual
            </button>
          </div>
        </div>
      </div>
    </section>
  `
})
export class PricingHeroComponent {
  @Input({ required: true }) billingInterval!: BillingInterval;
  @Output() billingIntervalChange = new EventEmitter<BillingInterval>();
}
