import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-public-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <footer class="hrcore-brand-theme border-t border-border bg-sidebar text-primary-foreground">
      <div class="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 lg:grid-cols-[1.25fr_0.85fr_0.85fr_1fr] lg:px-8 lg:py-14">
        <div>
          <div class="flex items-center gap-3">
            <span class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-card shadow-sm">
              <img src="/logo.png" alt="HR Core" class="h-9 w-9 object-contain" />
            </span>

            <div>
              <p class="text-lg font-black leading-none">HR Core</p>
              <p class="mt-1 text-[11px] font-black uppercase tracking-[0.24em] text-primary-foreground/55">
                Plataforma de recursos humanos
              </p>
            </div>
          </div>

          <p class="mt-5 max-w-sm text-sm leading-7 text-primary-foreground/65">
            Plataforma SaaS para administrar tenants, usuarios, roles, permisos y suscripciones.
          </p>

          <div class="mt-6 flex gap-3">
            <a href="#" aria-label="Facebook" class="flex h-10 w-10 items-center justify-center rounded-2xl bg-card/10 text-primary-foreground transition-colors hover:bg-primary">
              <lucide-icon name="facebook" class="h-4 w-4"></lucide-icon>
            </a>
            <a href="#" aria-label="Instagram" class="flex h-10 w-10 items-center justify-center rounded-2xl bg-card/10 text-primary-foreground transition-colors hover:bg-primary">
              <lucide-icon name="instagram" class="h-4 w-4"></lucide-icon>
            </a>
            <a href="#" aria-label="YouTube" class="flex h-10 w-10 items-center justify-center rounded-2xl bg-card/10 text-primary-foreground transition-colors hover:bg-primary">
              <lucide-icon name="youtube" class="h-4 w-4"></lucide-icon>
            </a>
          </div>
        </div>

        <div>
          <h3 class="text-sm font-black uppercase tracking-[0.18em] text-primary-foreground/55">Navegación</h3>
          <ul class="mt-5 space-y-3 text-sm text-primary-foreground/72">
            <li><a routerLink="/" fragment="inicio" class="transition-colors hover:text-primary-foreground">Inicio</a></li>
            <li><a routerLink="/" fragment="beneficios" class="transition-colors hover:text-primary-foreground">Beneficios</a></li>
            <!-- <li><a routerLink="/" fragment="tutoriales" class="transition-colors hover:text-primary-foreground">Tutoriales</a></li> -->
            <!-- <li><a routerLink="/" fragment="plataforma-web" class="transition-colors hover:text-primary-foreground">Plataforma web</a></li> -->
            <!-- <li><a routerLink="/" fragment="app-movil" class="transition-colors hover:text-primary-foreground">App móvil</a></li> -->
            <li><a routerLink="/prices" class="transition-colors hover:text-primary-foreground">Precios</a></li>
          </ul>
        </div>

        <div>
          <h3 class="text-sm font-black uppercase tracking-[0.18em] text-primary-foreground/55">Accesos</h3>
          <ul class="mt-5 space-y-3 text-sm text-primary-foreground/72">
            <li><a routerLink="/login" class="transition-colors hover:text-primary-foreground">Login tenant</a></li>
            <li><a routerLink="/platform/login" class="transition-colors hover:text-primary-foreground">Login superadmin</a></li>
            <li><a routerLink="/onboarding" [queryParams]="{ plan: 'DEMO' }" class="transition-colors hover:text-primary-foreground">Crear demo</a></li>
            <li><a routerLink="/forgot-password" class="transition-colors hover:text-primary-foreground">Recuperar contraseña</a></li>
          </ul>
        </div>

        <div>
          <h3 class="text-sm font-black uppercase tracking-[0.18em] text-primary-foreground/55">Comienza ahora</h3>
          <p class="mt-5 text-sm leading-7 text-primary-foreground/65">
            Crea un tenant demo o revisa los planes disponibles para iniciar con una suscripción pagada.
          </p>
          <div class="mt-5 grid gap-3">
            <a routerLink="/prices" class="inline-flex h-11 items-center justify-center gap-2 rounded-2xl bg-primary px-5 text-sm font-black text-primary-foreground transition-colors hover:bg-primary/90">
              Ver planes
              <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
            </a>
            <a routerLink="/onboarding" [queryParams]="{ plan: 'DEMO' }" class="inline-flex h-11 items-center justify-center rounded-2xl border border-border/18 bg-card/8 px-5 text-sm font-black text-primary-foreground transition-colors hover:bg-card/12">
              Crear demo gratis
            </a>
          </div>
        </div>
      </div>

      <div class="border-t border-border/10">
        <div class="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-5 text-xs text-primary-foreground/45 sm:px-6 md:flex-row md:items-center md:justify-between lg:px-8">
          <p>© 2026 HR Core. Todos los derechos reservados.</p>
          <div class="flex flex-wrap gap-x-5 gap-y-2">
            <a href="#" class="transition-colors hover:text-primary-foreground">Términos</a>
            <a href="#" class="transition-colors hover:text-primary-foreground">Privacidad</a>
            <a href="#" class="transition-colors hover:text-primary-foreground">Soporte</a>
          </div>
        </div>
      </div>
    </footer>
  `
})
export class PublicFooterComponent {}
