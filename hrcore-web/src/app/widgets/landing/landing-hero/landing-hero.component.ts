import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-landing-hero',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section id="inicio" class="relative overflow-hidden bg-[#F3FAF1]">
      <div
        class="absolute left-1/2 top-0 h-[520px] w-[520px] -translate-x-1/2 rounded-full bg-white/70 blur-3xl"
      ></div>

      <div
        class="relative mx-auto grid max-w-7xl gap-12 px-4 pb-16 pt-12 sm:px-6 lg:grid-cols-[1.08fr_0.92fr] lg:px-8 lg:pb-20 lg:pt-16"
      >
        <div class="flex flex-col justify-center">
          <div
            class="mb-8 inline-flex w-fit items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-5 py-2 text-xs font-black uppercase tracking-[0.22em] text-[#2E7D32] shadow-sm"
          >
            <lucide-icon name="sparkles" class="h-4 w-4"></lucide-icon>
            Gestión humana simple y segura
          </div>

          <h1
            class="max-w-4xl text-5xl font-black leading-[0.98] tracking-tight text-[#083B16] sm:text-6xl lg:text-7xl"
          >
            Una experiencia más clara para gestionar personas, talento y equipos.
          </h1>

          <p class="mt-8 max-w-3xl text-lg leading-9 text-[#49624D]">
            Entra como usuario de tu organización o como superadmin, administra
            colaboradores, roles y permisos, y aprende con guías breves a usar
            toda la plataforma.
          </p>

          <div class="mt-10 flex flex-col gap-3 sm:flex-row">
            <a
              routerLink="/prices"
              class="inline-flex items-center justify-center gap-3 rounded-2xl border border-[#C8E6C9] bg-white px-7 py-4 text-sm font-black text-[#2E7D32] shadow-sm transition-colors hover:bg-[#F1F8E9]"
            >
              <lucide-icon name="dollar-sign" class="h-4 w-4"></lucide-icon>
              Ver planes
            </a>

            <a
              routerLink="/login"
              class="inline-flex items-center justify-center gap-3 rounded-2xl bg-[#2E7D32] px-7 py-4 text-sm font-black text-white shadow-sm transition-colors hover:bg-[#256428]"
            >
              <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
              Ingresar a mi organización
            </a>

            <a
              routerLink="/platform/login"
              class="inline-flex items-center justify-center gap-3 rounded-2xl border border-[#C8E6C9] bg-white px-7 py-4 text-sm font-black text-[#2E7D32] shadow-sm transition-colors hover:bg-[#F1F8E9]"
            >
              <lucide-icon name="shield-check" class="h-4 w-4"></lucide-icon>
              superadmin
            </a>
          </div>
        </div>

        <div class="flex items-center">
          <div
            class="w-full rounded-[32px] border border-[#D6E8D1] bg-white/90 p-8 shadow-[0_30px_90px_rgba(27,94,32,0.12)]"
          >
            <div class="flex items-start justify-between gap-6">
              <div>
                <p
                  class="text-sm font-black uppercase tracking-[0.24em] text-[#7A8C7C]"
                >
                  Inicio rápido
                </p>
                <h2
                  class="mt-4 text-3xl font-black leading-tight text-[#083B16]"
                >
                  Conoce la plataforma y empieza con confianza
                </h2>
              </div>

              <div
                class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]"
              >
                <lucide-icon name="book-open" class="h-7 w-7"></lucide-icon>
              </div>
            </div>

            <div class="mt-8 space-y-5">
              <div class="rounded-2xl border border-[#D6E8D1] bg-[#FAFCF8] p-5">
                <p
                  class="text-xs font-black uppercase tracking-[0.22em] text-[#7A8C7C]"
                >
                  01. Acceso seguro
                </p>
                <p class="mt-3 text-sm leading-7 text-[#49624D]">
                  Ingresa con contraseña o con rostro, según tu configuración y
                  el tenant activo.
                </p>
              </div>

              <div class="rounded-2xl border border-[#D6E8D1] bg-[#FAFCF8] p-5">
                <p
                  class="text-xs font-black uppercase tracking-[0.22em] text-[#7A8C7C]"
                >
                  02. Personas y equipos
                </p>
                <p class="mt-3 text-sm leading-7 text-[#49624D]">
                  Organiza colaboradores, equipos y responsabilidades desde una
                  sola interfaz.
                </p>
              </div>

              <div class="rounded-2xl border border-[#D6E8D1] bg-[#FAFCF8] p-5">
                <p
                  class="text-xs font-black uppercase tracking-[0.22em] text-[#7A8C7C]"
                >
                  03. Tu perfil
                </p>
                <p class="mt-3 text-sm leading-7 text-[#49624D]">
                  Mantén tu perfil actualizado y accede según los permisos de tu
                  organización.
                </p>
              </div>
            </div>

            <div class="mt-7 flex flex-col gap-3 sm:flex-row">
              <!--
              <a
                href="#tutoriales"
                class="inline-flex items-center justify-center gap-2 rounded-2xl border border-[#C8E6C9] bg-white px-5 py-3 text-sm font-black text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]"
              >
                <lucide-icon name="play" class="h-4 w-4"></lucide-icon>
                Ver tutoriales
              </a>

              <a
                href="https://youtube.com"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center justify-center gap-2 rounded-2xl bg-[#1B6B2A] px-5 py-3 text-sm font-black text-white transition-colors hover:bg-[#145421]"
              >
                <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
                Ir al canal de YouTube
              </a>
              -->
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class LandingHeroComponent {}
