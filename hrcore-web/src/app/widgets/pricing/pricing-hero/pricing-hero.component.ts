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
      <div class="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_18%_20%,rgba(46,125,50,0.12),transparent_30%),radial-gradient(circle_at_82%_20%,rgba(210,150,45,0.12),transparent_30%),linear-gradient(180deg,#F7FBF3_0%,#EEF7EA_100%)]"></div>

      <div class="mx-auto max-w-7xl">
        <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-3xl">
            <p class="text-sm font-black uppercase tracking-[0.24em] text-[#2E7D32]">Precios</p>

            <h1 class="mt-4 text-4xl font-black leading-tight tracking-tight text-[#083B16] sm:text-5xl lg:text-6xl">Elige el plan para crear tu tenant.</h1>

            <p class="mt-5 max-w-2xl text-base leading-8 text-[#405447]">Inicia gratis con DEMO o crea tu organización con una suscripción pagada usando Stripe desde el registro.</p>
          </div>

          <div class="inline-flex w-fit rounded-full border border-[#C8E6C9] bg-white p-1 shadow-sm">
            <button
              type="button"
              (click)="billingIntervalChange.emit('MONTHLY')"
              class="h-10 rounded-full px-6 text-sm font-black transition-colors"
              [class.bg-[#2E7D32]]="billingInterval === 'MONTHLY'"
              [class.text-white]="billingInterval === 'MONTHLY'"
              [class.text-[#2E7D32]]="billingInterval !== 'MONTHLY'">
              Mensual
            </button>

            <button
              type="button"
              (click)="billingIntervalChange.emit('YEARLY')"
              class="h-10 rounded-full px-6 text-sm font-black transition-colors"
              [class.bg-[#2E7D32]]="billingInterval === 'YEARLY'"
              [class.text-white]="billingInterval === 'YEARLY'"
              [class.text-[#2E7D32]]="billingInterval !== 'YEARLY'">
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
