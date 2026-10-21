import { CommonModule } from '@angular/common';
import { Component, HostListener, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { PublicNavItem } from './public-nav-item.model';

@Component({
  selector: 'app-public-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <header
      class="hrcore-brand-theme sticky top-0 z-50 border-b border-border/80 bg-card/90 shadow-sm backdrop-blur-xl transition-all duration-300"
    >
      <nav
        class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:h-[76px] lg:px-8"
      >
        <a
          routerLink="/"
          fragment="inicio"
          (click)="closeMobileMenu()"
          class="group flex min-w-0 items-center gap-3 text-left transition-transform duration-200 hover:scale-[1.01]"
        >
          <span
            class="flex h-11 w-11 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-border bg-background shadow-sm transition-all duration-300 group-hover:border-primary/40 group-hover:shadow-md"
          >
            <img
              src="/logo.png"
              alt="HR Core"
              class="h-9 w-9 object-contain transition-transform duration-300 group-hover:scale-105"
            />
          </span>

          <span class="min-w-0">
            <span
              class="block truncate text-base font-black leading-none text-accent-foreground transition-colors group-hover:text-primary sm:text-lg"
            >
              HR Core
            </span>
            <span
              class="mt-1 hidden text-[11px] font-black uppercase tracking-[0.28em] text-muted-foreground sm:block"
            >
              Plataforma de recursos humanos
            </span>
          </span>
        </a>

        <div class="hidden items-center gap-2 lg:flex">
          @for (item of navItems; track item.label) {
            <a
              [routerLink]="item.routerLink"
              [fragment]="item.fragment"
              class="relative rounded-full px-4 py-2 text-sm font-black text-foreground transition-all duration-200 hover:bg-accent hover:text-primary"
            >
              {{ item.label }}
            </a>
          }
        </div>

        <div class="hidden items-center gap-3 lg:flex">
          <a
            routerLink="/prices"
            class="inline-flex h-11 items-center justify-center rounded-full border border-border bg-card px-5 text-sm font-black text-primary shadow-xs transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:bg-accent hover:shadow-sm"
          >
            Registrarse
          </a>
          <a
            routerLink="/login"
            class="inline-flex h-11 items-center justify-center gap-2 rounded-full bg-primary px-6 text-sm font-black text-primary-foreground shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-primary/90 hover:shadow-md"
          >
            <span>Ingresar</span>
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>
        </div>

        <button
          type="button"
          (click)="toggleMobileMenu()"
          class="inline-flex h-11 w-11 cursor-pointer items-center justify-center rounded-2xl border border-border bg-card text-accent-foreground shadow-sm transition-all duration-200 hover:border-primary/40 hover:bg-accent lg:hidden"
          [attr.aria-label]="mobileMenuOpen() ? 'Cerrar menú' : 'Abrir menú'"
          [attr.aria-expanded]="mobileMenuOpen()"
        >
          <lucide-icon
            [name]="mobileMenuOpen() ? 'x' : 'menu'"
            class="h-5 w-5 transition-transform duration-200"
          ></lucide-icon>
        </button>
      </nav>

      @if (mobileMenuOpen()) {
        <div
          class="animate-scale-in border-t border-border bg-card px-4 pb-5 shadow-xl lg:hidden"
        >
          <div class="mx-auto max-w-7xl pt-3">
            <div class="grid gap-1.5">
              @for (item of navItems; track item.label) {
                <a
                  [routerLink]="item.routerLink"
                  [fragment]="item.fragment"
                  (click)="closeMobileMenu()"
                  class="rounded-2xl px-4 py-3 text-left text-sm font-black text-accent-foreground transition-all duration-200 hover:translate-x-1 hover:bg-accent hover:text-primary"
                >
                  {{ item.label }}
                </a>
              }

              <div class="mt-3 grid gap-2 border-t border-border pt-4">
                <a
                  routerLink="/login"
                  (click)="closeMobileMenu()"
                  class="inline-flex h-11 items-center justify-center gap-2 rounded-2xl bg-primary px-5 text-sm font-black text-primary-foreground shadow-sm transition-all hover:bg-primary/90"
                >
                  Ingresar
                  <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
                </a>

                <a
                  routerLink="/prices"
                  (click)="closeMobileMenu()"
                  class="inline-flex h-11 items-center justify-center rounded-2xl border border-border bg-card px-5 text-sm font-black text-primary transition-colors hover:bg-accent"
                >
                  Ver planes y registro
                </a>

                <a
                  routerLink="/platform/login"
                  (click)="closeMobileMenu()"
                  class="inline-flex h-11 items-center justify-center rounded-2xl border border-border bg-card px-5 text-sm font-black text-accent-foreground transition-colors hover:bg-background"
                >
                  Superadmin
                </a>
              </div>
            </div>
          </div>
        </div>
      }
    </header>
  `,
})
export class PublicNavbarComponent {
  readonly mobileMenuOpen = signal(false);

  readonly navItems: PublicNavItem[] = [
    { label: 'Inicio', routerLink: '/', fragment: 'inicio' },
    { label: 'Beneficios', routerLink: '/', fragment: 'beneficios' },
    // { label: 'Tutoriales', routerLink: '/', fragment: 'tutoriales' },
    // { label: 'Plataforma', routerLink: '/', fragment: 'plataforma-web' },
    { label: 'App móvil', routerLink: '/', fragment: 'app-movil' },
    { label: 'Manual', routerLink: '/manual' },
    { label: 'Precios', routerLink: '/', fragment: 'precios' },
  ];

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth >= 1024) {
      this.closeMobileMenu();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeMobileMenu();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update((value) => !value);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
