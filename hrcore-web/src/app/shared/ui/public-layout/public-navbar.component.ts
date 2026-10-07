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
      class="sticky top-0 z-50 border-b border-[#DDEED8] bg-white/95 shadow-[0_8px_24px_rgba(27,94,32,0.06)] backdrop-blur-xl"
    >
      <nav
        class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:h-[76px] lg:px-8"
      >
        <a
          routerLink="/"
          fragment="inicio"
          (click)="closeMobileMenu()"
          class="flex min-w-0 items-center gap-3 text-left"
        >
          <span
            class="flex h-11 w-11 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-[#C8E6C9] bg-[#F3FAF1] shadow-sm"
          >
            <img
              src="/logo.png"
              alt="HR Core"
              class="h-9 w-9 object-contain"
            />
          </span>

          <span class="min-w-0">
            <span
              class="block truncate text-base font-black leading-none text-[#083B16] sm:text-lg"
            >
              HR Core
            </span>
            <span
              class="mt-1 hidden text-[11px] font-black uppercase tracking-[0.28em] text-[#6B7D6C] sm:block"
            >
              Plataforma de recursos humanos
            </span>
          </span>
        </a>

        <div class="hidden items-center gap-8 lg:flex">
          @for (item of navItems; track item.label) {
            <a
              [routerLink]="item.routerLink"
              [fragment]="item.fragment"
              class="text-sm font-black text-[#2E5F37] transition-colors hover:text-[#1B5E20]"
            >
              {{ item.label }}
            </a>
          }
        </div>

        <div class="hidden items-center gap-3 lg:flex">
          <a
            routerLink="/login"
            class="inline-flex h-11 items-center justify-center rounded-full bg-[#2E7D32] px-6 text-sm font-black text-white shadow-sm transition-colors hover:bg-[#256428]"
          >
            Ingresar
          </a>
        </div>

        <button
          type="button"
          (click)="toggleMobileMenu()"
          class="inline-flex h-11 w-11 items-center justify-center rounded-2xl border cursor-pointer border-[#C8E6C9] bg-white text-[#1B5E20] shadow-sm transition-colors hover:bg-[#F3FAF1] lg:hidden"
          [attr.aria-label]="mobileMenuOpen() ? 'Cerrar menú' : 'Abrir menú'"
          [attr.aria-expanded]="mobileMenuOpen()"
        >
          <lucide-icon
            [name]="mobileMenuOpen() ? 'x' : 'menu'"
            class="h-5 w-5"
          ></lucide-icon>
        </button>
      </nav>

      @if (mobileMenuOpen()) {
        <div
          class="border-t border-[#DDEED8] bg-white px-4 pb-5 shadow-xl lg:hidden"
        >
          <div class="mx-auto max-w-7xl pt-3">
            <div class="grid gap-2">
              @for (item of navItems; track item.label) {
                <a
                  [routerLink]="item.routerLink"
                  [fragment]="item.fragment"
                  (click)="closeMobileMenu()"
                  class="rounded-2xl px-4 py-3 text-left text-sm font-black text-[#1B5E20] transition-colors hover:bg-[#F3FAF1]"
                >
                  {{ item.label }}
                </a>
              }

              <div class="mt-3 grid gap-2 border-t border-[#E8F2E2] pt-4">
                <a
                  routerLink="/login"
                  (click)="closeMobileMenu()"
                  class="inline-flex h-11 items-center justify-center rounded-2xl bg-[#2E7D32] px-5 text-sm font-black text-white transition-colors hover:bg-[#256428]"
                >
                  Ingresar
                </a>

                <a
                  routerLink="/platform/login"
                  (click)="closeMobileMenu()"
                  class="inline-flex h-11 items-center justify-center rounded-2xl border border-[#C8E6C9] bg-white px-5 text-sm font-black text-[#1B5E20] transition-colors hover:bg-[#F3FAF1]"
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
    // { label: 'App móvil', routerLink: '/', fragment: 'app-movil' },
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
