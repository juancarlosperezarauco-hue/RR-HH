import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-landing-hero',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section id="inicio" class="relative overflow-hidden bg-gradient-to-b from-accent/60 via-background to-background">
      <div
        class="pointer-events-none absolute -top-24 left-1/4 h-[440px] w-[440px] rounded-full bg-primary/10 blur-3xl"
      ></div>
      <div
        class="pointer-events-none absolute right-10 top-20 h-[360px] w-[360px] rounded-full bg-secondary/10 blur-3xl"
      ></div>

      <div
        class="relative mx-auto grid max-w-7xl gap-12 px-4 pb-16 pt-12 sm:px-6 lg:grid-cols-[1.08fr_0.92fr] lg:px-8 lg:pb-20 lg:pt-16"
      >
        <div class="animate-slide-up flex flex-col justify-center">
          <div
            class="mb-7 inline-flex w-fit items-center gap-2 rounded-full border border-primary/20 bg-card/90 px-4 py-2 text-xs font-black uppercase tracking-[0.22em] text-primary shadow-xs backdrop-blur-xs transition-all duration-300 hover:border-primary/40 hover:shadow-sm"
          >
            <lucide-icon name="sparkles" class="h-4 w-4 text-secondary"></lucide-icon>
            Gestión humana simple, moderna y segura
          </div>

          <h1
            class="max-w-4xl text-4xl font-black leading-[1.02] tracking-tight text-accent-foreground sm:text-5xl lg:text-6xl"
          >
            Una experiencia más clara para gestionar
            <span class="bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">
              personas, talento y equipos
            </span>
          </h1>

          <p class="mt-6 max-w-2xl text-base leading-8 text-muted-foreground sm:text-lg">
            Centraliza la administración de colaboradores, estructura organizativa, turnos, asistencia, contratos y permisos con seguridad multi-tenant y acceso biométrico facial.
          </p>

          <div class="mt-8 flex flex-wrap gap-3">
            <a
              routerLink="/login"
              class="group inline-flex items-center justify-center gap-3 rounded-2xl bg-primary px-7 py-4 text-sm font-black text-primary-foreground shadow-md transition-all duration-200 hover:-translate-y-0.5 hover:bg-primary/90 hover:shadow-lg"
            >
              <span>Ingresar a mi organización</span>
              <lucide-icon name="arrow-right" class="h-4 w-4 transition-transform duration-200 group-hover:translate-x-1"></lucide-icon>
            </a>

            <a
              routerLink="/prices"
              class="inline-flex items-center justify-center gap-2.5 rounded-2xl border border-border bg-card px-6 py-4 text-sm font-black text-primary shadow-xs transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:bg-accent hover:shadow-md"
            >
              <lucide-icon name="dollar-sign" class="h-4 w-4"></lucide-icon>
              Ver planes y registro
            </a>

            <a
              routerLink="/platform/login"
              class="inline-flex items-center justify-center gap-2.5 rounded-2xl border border-border bg-card/80 px-5 py-4 text-sm font-black text-accent-foreground shadow-xs transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:bg-accent hover:text-primary"
            >
              <lucide-icon name="shield-check" class="h-4 w-4 text-primary"></lucide-icon>
              Superadmin
            </a>
          </div>

          <div class="mt-10 grid grid-cols-2 gap-3 border-t border-border/80 pt-7 sm:grid-cols-4">
            <div class="rounded-2xl border border-border/70 bg-card/70 px-3.5 py-3 backdrop-blur-xs transition-all duration-200 hover:border-primary/30 hover:bg-card">
              <p class="text-xs font-black uppercase tracking-wider text-primary">Multi-Tenant</p>
              <p class="mt-1 text-xs font-medium text-muted-foreground">Esquemas aislados</p>
            </div>
            <div class="rounded-2xl border border-border/70 bg-card/70 px-3.5 py-3 backdrop-blur-xs transition-all duration-200 hover:border-primary/30 hover:bg-card">
              <p class="text-xs font-black uppercase tracking-wider text-primary">Biometría</p>
              <p class="mt-1 text-xs font-medium text-muted-foreground">Acceso por rostro</p>
            </div>
            <div class="rounded-2xl border border-border/70 bg-card/70 px-3.5 py-3 backdrop-blur-xs transition-all duration-200 hover:border-primary/30 hover:bg-card">
              <p class="text-xs font-black uppercase tracking-wider text-primary">RBAC Granular</p>
              <p class="mt-1 text-xs font-medium text-muted-foreground">+170 permisos</p>
            </div>
            <div class="rounded-2xl border border-border/70 bg-card/70 px-3.5 py-3 backdrop-blur-xs transition-all duration-200 hover:border-primary/30 hover:bg-card">
              <p class="text-xs font-black uppercase tracking-wider text-primary">Normativa</p>
              <p class="mt-1 text-xs font-medium text-muted-foreground">Adaptado a LGT</p>
            </div>
          </div>
        </div>

        <div class="animate-slide-up flex items-center">
          <div
            class="w-full rounded-[32px] border border-border bg-card/95 p-7 shadow-xl backdrop-blur-sm transition-all duration-300 hover:shadow-2xl sm:p-8"
          >
            <div class="flex items-start justify-between gap-5">
              <div>
                <p
                  class="text-xs font-black uppercase tracking-[0.24em] text-primary"
                >
                  Inicio rápido
                </p>
                <h2
                  class="mt-2.5 text-2xl font-black leading-tight text-accent-foreground sm:text-3xl"
                >
                  Conoce la plataforma y empieza con confianza
                </h2>
              </div>

              <div
                class="animate-float flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-accent text-primary shadow-xs"
              >
                <lucide-icon name="book-open" class="h-7 w-7"></lucide-icon>
              </div>
            </div>

            <div class="mt-7 space-y-4">
              <div class="group rounded-2xl border border-border bg-muted/60 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:bg-accent/50 hover:shadow-sm">
                <div class="flex items-center gap-2.5">
                  <span class="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-xs font-black text-primary">01</span>
                  <p class="text-xs font-black uppercase tracking-[0.18em] text-accent-foreground">
                    Acceso seguro y biométrico
                  </p>
                </div>
                <p class="mt-2 text-sm leading-6 text-muted-foreground">
                  Ingresa con contraseña o reconocimiento facial en segundos según el identificador de tu organización.
                </p>
              </div>

              <div class="group rounded-2xl border border-border bg-muted/60 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:bg-accent/50 hover:shadow-sm">
                <div class="flex items-center gap-2.5">
                  <span class="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-xs font-black text-primary">02</span>
                  <p class="text-xs font-black uppercase tracking-[0.18em] text-accent-foreground">
                    Personas, contratos y asistencia
                  </p>
                </div>
                <p class="mt-2 text-sm leading-6 text-muted-foreground">
                  Organiza departamentos, cargos, fichas laborales, turnos, marcaciones e incidencias desde un solo panel.
                </p>
              </div>

              <div class="group rounded-2xl border border-border bg-muted/60 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:bg-accent/50 hover:shadow-sm">
                <div class="flex items-center gap-2.5">
                  <span class="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-xs font-black text-primary">03</span>
                  <p class="text-xs font-black uppercase tracking-[0.18em] text-accent-foreground">
                    Auditoría y control en tiempo real
                  </p>
                </div>
                <p class="mt-2 text-sm leading-6 text-muted-foreground">
                  Visualiza estadísticas operativas, reportes ejecutivos y bitácora de seguridad con trazabilidad completa.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class LandingHeroComponent {}
