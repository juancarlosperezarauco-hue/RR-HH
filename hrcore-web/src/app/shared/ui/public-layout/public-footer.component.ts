import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-public-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <footer class="border-t border-[#DDEED8] bg-[#102016] text-white">
      <div class="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 lg:grid-cols-[1.25fr_0.85fr_0.85fr_1fr] lg:px-8 lg:py-14">
        <div>
          <div class="flex items-center gap-3">
            <span class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-white shadow-sm">
              <img src="/logo.png" alt="HR Core" class="h-9 w-9 object-contain" />
            </span>

            <div>
              <p class="text-lg font-black leading-none">HR Core</p>
              <p class="mt-1 text-[11px] font-black uppercase tracking-[0.24em] text-white/55">
                Plataforma de recursos humanos
              </p>
            </div>
          </div>

          <p class="mt-5 max-w-sm text-sm leading-7 text-white/65">
            Plataforma SaaS para administrar tenants, usuarios, roles, permisos y suscripciones.
          </p>

          <div class="mt-6 flex gap-3">
            <a href="#" aria-label="Facebook" class="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/10 text-white transition-colors hover:bg-[#2E7D32]">
              <lucide-icon name="facebook" class="h-4 w-4"></lucide-icon>
            </a>
            <a href="#" aria-label="Instagram" class="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/10 text-white transition-colors hover:bg-[#2E7D32]">
              <lucide-icon name="instagram" class="h-4 w-4"></lucide-icon>
            </a>
            <a href="#" aria-label="YouTube" class="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/10 text-white transition-colors hover:bg-[#2E7D32]">
              <lucide-icon name="youtube" class="h-4 w-4"></lucide-icon>
            </a>
          </div>
        </div>

        <div>
          <h3 class="text-sm font-black uppercase tracking-[0.18em] text-white/55">Navegación</h3>
          <ul class="mt-5 space-y-3 text-sm text-white/72">
            <li><a routerLink="/" fragment="inicio" class="transition-colors hover:text-white">Inicio</a></li>
            <li><a routerLink="/" fragment="beneficios" class="transition-colors hover:text-white">Beneficios</a></li>
            <!-- <li><a routerLink="/" fragment="tutoriales" class="transition-colors hover:text-white">Tutoriales</a></li> -->
            <!-- <li><a routerLink="/" fragment="plataforma-web" class="transition-colors hover:text-white">Plataforma web</a></li> -->
            <!-- <li><a routerLink="/" fragment="app-movil" class="transition-colors hover:text-white">App móvil</a></li> -->
            <li><a routerLink="/prices" class="transition-colors hover:text-white">Precios</a></li>
          </ul>
        </div>

        <div>
          <h3 class="text-sm font-black uppercase tracking-[0.18em] text-white/55">Accesos</h3>
          <ul class="mt-5 space-y-3 text-sm text-white/72">
            <li><a routerLink="/login" class="transition-colors hover:text-white">Login tenant</a></li>
            <li><a routerLink="/platform/login" class="transition-colors hover:text-white">Login superadmin</a></li>
            <li><a routerLink="/onboarding" [queryParams]="{ plan: 'DEMO' }" class="transition-colors hover:text-white">Crear demo</a></li>
            <li><a routerLink="/forgot-password" class="transition-colors hover:text-white">Recuperar contraseña</a></li>
          </ul>
        </div>

        <div>
          <h3 class="text-sm font-black uppercase tracking-[0.18em] text-white/55">Comienza ahora</h3>
          <p class="mt-5 text-sm leading-7 text-white/65">
            Crea una organización y selecciona directamente el plan que utilizará, sin registrar tarjeta.
          </p>
          <div class="mt-5 grid gap-3">
            <a routerLink="/prices" class="inline-flex h-11 items-center justify-center gap-2 rounded-2xl bg-[#2E7D32] px-5 text-sm font-black text-white transition-colors hover:bg-[#256428]">
              Ver planes
              <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
            </a>
            <a routerLink="/onboarding" [queryParams]="{ plan: 'DEMO' }" class="inline-flex h-11 items-center justify-center rounded-2xl border border-white/18 bg-white/8 px-5 text-sm font-black text-white transition-colors hover:bg-white/12">
              Crear demo gratis
            </a>
          </div>
        </div>
      </div>

      <div class="border-t border-white/10">
        <div class="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-5 text-xs text-white/45 sm:px-6 md:flex-row md:items-center md:justify-between lg:px-8">
          <p>© 2026 HR Core. Todos los derechos reservados.</p>
          <div class="flex flex-wrap gap-x-5 gap-y-2">
            <a href="#" class="transition-colors hover:text-white">Términos</a>
            <a href="#" class="transition-colors hover:text-white">Privacidad</a>
            <a href="#" class="transition-colors hover:text-white">Soporte</a>
          </div>
        </div>
      </div>
    </footer>
  `
})
export class PublicFooterComponent {}
