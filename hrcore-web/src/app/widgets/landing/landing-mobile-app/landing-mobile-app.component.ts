import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-landing-mobile-app',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section id="app-movil" class="bg-[#F7FBF3] px-4 py-20 sm:px-6 lg:px-8">
      <div class="mx-auto grid max-w-7xl gap-10 lg:grid-cols-[1fr_0.9fr] lg:items-center">
        <div class="max-w-3xl">
          <div class="mb-5 inline-flex w-fit items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-5 py-2 text-sm font-black text-[#2E7D32] shadow-sm">
            <lucide-icon name="smartphone" class="h-4 w-4"></lucide-icon>
            Aplicación móvil
          </div>

          <h2 class="text-4xl font-black tracking-tight text-[#101827] sm:text-5xl">
            HR Core también en tu móvil
          </h2>

          <p class="mt-5 max-w-2xl text-base leading-8 text-[#405447]">
            Descarga la app móvil desde Drive y gestiona personas, equipos, perfil y acceso seguro desde tu dispositivo.
          </p>

          <div class="mt-10 grid gap-5 sm:grid-cols-3">
            <article class="rounded-[28px] border border-[#DDEED8] bg-white p-6 shadow-sm">
              <div class="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
                <lucide-icon name="shield-check" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="text-lg font-black text-[#101827]">Acceso seguro</h3>
              <p class="mt-2 text-sm leading-7 text-[#405447]">
                Ingresa con tus credenciales y mantén tu sesión protegida.
              </p>
            </article>

            <article class="rounded-[28px] border border-[#DDEED8] bg-white p-6 shadow-sm">
              <div class="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
                <lucide-icon name="credit-card" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="text-lg font-black text-[#101827]">Gestión de personas</h3>
              <p class="mt-2 text-sm leading-7 text-[#405447]">
                Consulta perfiles, equipos y tareas desde el móvil.
              </p>
            </article>

            <article class="rounded-[28px] border border-[#DDEED8] bg-white p-6 shadow-sm">
              <div class="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
                <lucide-icon name="bell" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="text-lg font-black text-[#101827]">Notificaciones</h3>
              <p class="mt-2 text-sm leading-7 text-[#405447]">
                Revisa avisos importantes de tu cuenta.
              </p>
            </article>
          </div>

          <div class="mt-10 flex flex-col gap-3 sm:flex-row">
            <a
              href="https://drive.google.com"
              target="_blank"
              rel="noopener noreferrer"
              class="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#2E7D32] px-5 text-sm font-black text-white shadow-sm transition-colors hover:bg-[#256428]">
              Descargar desde Drive
              <lucide-icon name="download" class="h-4 w-4"></lucide-icon>
            </a>

            <a
              href="#tutoriales"
              class="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-[#C8E6C9] bg-white px-5 text-sm font-black text-[#1B5E20] shadow-sm transition-colors hover:bg-[#F7FBF3]">
              Ver tutoriales
              <lucide-icon name="play" class="h-4 w-4"></lucide-icon>
            </a>
          </div>
        </div>

        <div class="flex justify-center lg:justify-end">
          <div class="relative w-full max-w-md">
            <div class="absolute -left-5 -top-5 h-24 w-24 rotate-12 rounded-3xl bg-[#2E7D32]/12"></div>
            <div class="absolute -bottom-5 -right-5 h-28 w-28 -rotate-12 rounded-3xl bg-[#D2962D]/16"></div>

            <div class="relative rounded-[34px] border border-[#DDEED8] bg-white p-7 text-center shadow-[0_30px_90px_rgba(27,94,32,0.16)]">
              <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-[#2E7D32] text-white">
                <lucide-icon name="qr-code" class="h-8 w-8"></lucide-icon>
              </div>

              <h3 class="mt-6 text-2xl font-black text-[#101827]">Escanea y descarga</h3>
              <p class="mx-auto mt-3 max-w-xs text-sm leading-6 text-[#405447]">
                Usa la cámara de tu teléfono para abrir el enlace de descarga.
              </p>

              <div class="mx-auto mt-7 flex max-w-[250px] items-center justify-center rounded-[28px] border border-[#E0EEDF] bg-[#FAFCF8] p-5 shadow-inner">
                <img
                  src="/qr-app.png"
                  alt="Código QR para descargar la app móvil"
                  class="h-48 w-48 object-contain" />
              </div>

              <div class="mt-6 rounded-2xl border border-[#DDEED8] bg-[#F7FBF3] p-4 text-left">
                <p class="text-xs font-black uppercase tracking-[0.16em] text-[#6B7D6C]">Disponible</p>
                <p class="mt-2 text-sm font-bold text-[#101827]">Descarga directa para Android</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `
})
export class LandingMobileAppComponent {}
