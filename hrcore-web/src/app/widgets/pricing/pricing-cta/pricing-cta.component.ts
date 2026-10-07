import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-pricing-cta',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section class="bg-[#F7FBF3] px-4 pb-20 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-7xl">
        <div class="relative overflow-hidden rounded-[34px] bg-[#102016] p-8 text-white shadow-[0_28px_80px_rgba(16,32,22,0.25)] sm:p-10 lg:p-12">
          <div class="absolute inset-0 opacity-[0.16] [background-image:radial-gradient(#ffffff_1px,transparent_1px)] [background-size:28px_28px]"></div>

          <div class="relative grid gap-8 lg:grid-cols-[1fr_auto] lg:items-center">
            <div>
              <p class="text-sm font-black uppercase tracking-[0.22em] text-white/55">¿Listo para empezar?</p>

              <h2 class="mt-4 max-w-2xl text-3xl font-black tracking-tight sm:text-4xl">Crea tu tenant y empieza a gestionar personas con HR Core.</h2>

              <p class="mt-4 max-w-2xl text-sm leading-7 text-white/70">Puedes iniciar con DEMO o seleccionar un plan pagado desde el inicio.</p>
            </div>

            <div class="flex flex-col gap-3 sm:flex-row">
              <a routerLink="/onboarding" [queryParams]="{ plan: 'DEMO' }" class="inline-flex h-11 items-center justify-center rounded-xl bg-white px-5 text-sm font-black text-[#102016] transition-colors hover:bg-[#F3FAF1]">
                Crear demo
              </a>

              <a routerLink="/prices" class="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#2E7D32] px-5 text-sm font-black text-white transition-colors hover:bg-[#256428]">
                Ver planes
                <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  `
})
export class PricingCtaComponent {}
