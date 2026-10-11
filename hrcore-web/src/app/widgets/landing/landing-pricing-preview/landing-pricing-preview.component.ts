import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

interface LandingPreviewPlan {
  name: string;
  description: string;
  price: string;
  period: string;
  badge?: string;
  queryParams?: Record<string, string>;
  featured?: boolean;
}

@Component({
  selector: 'app-landing-pricing-preview',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section id="precios" class="bg-background px-4 py-20 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-7xl">
        <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-3xl">
            <div class="mb-5 inline-flex w-fit items-center gap-2 rounded-full border border-border bg-card px-5 py-2 text-sm font-black text-primary shadow-sm">
              <lucide-icon name="credit-card" class="h-4 w-4"></lucide-icon>
              Planes y suscripciones
            </div>

            <h2 class="text-4xl font-black tracking-tight text-foreground sm:text-5xl">
              Empieza gratis o activa tu tenant con pago
            </h2>

            <p class="mt-5 max-w-2xl text-base leading-8 text-muted-foreground">
              Elige un plan demo para probar la plataforma o continúa con una suscripción pagada usando Stripe desde el registro.
            </p>
          </div>

          <a
            routerLink="/prices"
            class="inline-flex h-11 w-fit items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground shadow-sm transition-colors hover:bg-primary/90">
            Ver todos los planes
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>
        </div>

        <div class="mt-12 grid gap-6 lg:grid-cols-3">
          @for (plan of plans; track plan.name) {
            <article
              class="relative flex min-h-[370px] flex-col rounded-[30px] border bg-card p-7 shadow-lg"
              [class.border-primary]="plan.featured"
              [class.ring-2]="plan.featured"
              [class.ring-secondary]="plan.featured"
              [class.border-border]="!plan.featured">

              @if (plan.badge) {
                <span class="absolute -top-4 left-7 rounded-full bg-primary px-4 py-2 text-xs font-black uppercase tracking-[0.14em] text-primary-foreground shadow-sm">
                  {{ plan.badge }}
                </span>
              }

              <h3 class="text-2xl font-black text-foreground">{{ plan.name }}</h3>
              <p class="mt-3 text-sm leading-7 text-muted-foreground">{{ plan.description }}</p>

              <div class="mt-8 flex items-end gap-2">
                <span class="text-4xl font-black text-foreground">{{ plan.price }}</span>
                <span class="pb-1 text-sm font-bold text-muted-foreground">{{ plan.period }}</span>
              </div>

              <ul class="mt-7 space-y-3">
                <li class="flex gap-3 text-sm font-semibold text-muted-foreground">
                  <lucide-icon name="check-circle" class="h-4 w-4 shrink-0 text-primary"></lucide-icon>
                  Usuarios según límite del plan
                </li>
                <li class="flex gap-3 text-sm font-semibold text-muted-foreground">
                  <lucide-icon name="check-circle" class="h-4 w-4 shrink-0 text-primary"></lucide-icon>
                  Roles y permisos configurables
                </li>
                <li class="flex gap-3 text-sm font-semibold text-muted-foreground">
                  <lucide-icon name="check-circle" class="h-4 w-4 shrink-0 text-primary"></lucide-icon>
                  Acceso a operación multi-tenant
                </li>
              </ul>

              <div class="mt-auto pt-8">
                <a
                  [routerLink]="plan.queryParams?.['plan'] === 'DEMO' ? '/onboarding' : '/prices'"
                  [queryParams]="plan.queryParams?.['plan'] === 'DEMO' ? plan.queryParams : null"
                  class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl text-sm font-black transition-colors"
                  [class.bg-primary]="plan.featured"
                  [class.text-primary-foreground]="plan.featured"
                  [class.hover:bg-primary/90]="plan.featured"
                  [class.bg-sidebar]="!plan.featured"
                  [class.text-primary-foreground]="!plan.featured"
                  [class.hover:bg-card]="!plan.featured">
                  Elegir {{ plan.name }}
                  <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
                </a>
              </div>
            </article>
          }
        </div>
      </div>
    </section>
  `
})
export class LandingPricingPreviewComponent {
  readonly plans: LandingPreviewPlan[] = [
    {
      name: 'Demo',
      description: 'Ideal para validar el sistema antes de contratar una suscripción.',
      price: 'USD 0',
      period: '/ prueba',
      queryParams: { plan: 'DEMO' }
    },
    {
      name: 'Basic',
      description: 'Para equipos pequeños que necesitan iniciar con una gestión humana ordenada.',
      price: 'Desde USD 9.99',
      period: '/ mes',
      queryParams: { plan: 'BASIC', billing: 'MONTHLY' }
    },
    {
      name: 'Professional',
      description: 'Para tenants que necesitan mayor capacidad operativa y crecimiento.',
      price: 'Desde USD 19.99',
      period: '/ mes',
      badge: 'Recomendado',
      featured: true,
      queryParams: { plan: 'PRO', billing: 'MONTHLY' }
    }
  ];
}
