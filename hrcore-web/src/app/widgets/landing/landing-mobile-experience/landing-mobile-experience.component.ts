import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-landing-mobile-experience',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section id="app-movil" class="relative isolate overflow-hidden bg-card px-4 py-20 sm:px-6 lg:px-8">
      <div class="absolute inset-0 -z-10 bg-accent"></div>

      <div class="mx-auto grid max-w-7xl items-center gap-12 lg:grid-cols-[1fr_0.9fr]">
        <div>
          <div class="mb-5 inline-flex w-fit items-center gap-2 rounded-full bg-accent px-5 py-2 text-sm font-black text-primary">
            <lucide-icon name="smartphone" class="h-4 w-4"></lucide-icon>
            Aplicación móvil
          </div>

          <h2 class="max-w-2xl text-4xl font-black tracking-tight text-foreground sm:text-5xl">
            Lleva HR Core también en tu teléfono
          </h2>

          <p class="mt-5 max-w-2xl text-base leading-8 text-muted-foreground">
            Descarga la app móvil desde Drive escaneando el código QR. Ideal para consultar personas, equipos y acceder a funciones rápidas desde el dispositivo.
          </p>

          <div class="mt-9 grid gap-5 sm:grid-cols-3">
            <div>
              <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-accent text-primary">
                <lucide-icon name="shield-check" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="font-black text-foreground">Acceso seguro</h3>
              <p class="mt-2 text-sm leading-6 text-muted-foreground">Ingresa con tu cuenta tenant y conserva el contexto de tu organización.</p>
            </div>

            <div>
              <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-accent text-primary">
                <lucide-icon name="credit-card" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="font-black text-foreground">Gestión de personas</h3>
              <p class="mt-2 text-sm leading-6 text-muted-foreground">Consulta perfiles y equipos desde una experiencia móvil.</p>
            </div>

            <div>
              <div class="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-accent text-primary">
                <lucide-icon name="bell" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="font-black text-foreground">Avisos</h3>
              <p class="mt-2 text-sm leading-6 text-muted-foreground">Mantén visible la información importante de tu cuenta.</p>
            </div>
          </div>

          <div class="mt-10 flex flex-col gap-3 sm:flex-row">
            <a
              href="https://drive.google.com"
              target="_blank"
              rel="noopener noreferrer"
              class="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground shadow-sm transition-colors hover:bg-primary/90">
              Descargar desde Drive
              <lucide-icon name="download" class="h-4 w-4"></lucide-icon>
            </a>

            <a
              href="#tutoriales"
              class="inline-flex h-11 items-center justify-center rounded-xl border border-border bg-card px-5 text-sm font-black text-accent-foreground shadow-sm transition-colors hover:bg-background">
              Ver tutoriales
            </a>
          </div>
        </div>

        <div class="flex justify-center lg:justify-end">
          <div class="relative w-full max-w-md">
            <div class="absolute -left-5 -top-5 h-24 w-24 rotate-12 rounded-3xl bg-primary/12"></div>
            <div class="absolute -bottom-5 -right-5 h-28 w-28 -rotate-12 rounded-3xl bg-secondary/16"></div>

            <div class="relative rounded-[34px] border border-border bg-card p-7 text-center shadow-lg">
              <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-primary text-primary-foreground">
                <lucide-icon name="qr-code" class="h-8 w-8"></lucide-icon>
              </div>

              <h3 class="mt-6 text-2xl font-black text-foreground">Escanea y descarga</h3>
              <p class="mx-auto mt-3 max-w-xs text-sm leading-6 text-muted-foreground">
                Usa la cámara de tu teléfono para abrir el enlace de descarga.
              </p>

              <div class="mx-auto mt-7 flex max-w-[250px] items-center justify-center rounded-[28px] border border-border bg-muted p-5 shadow-inner">
                <img
                  src="/qr-app.png"
                  alt="Código QR para descargar la app móvil"
                  class="h-48 w-48 object-contain" />
              </div>

              <div class="mt-6 rounded-2xl border border-border bg-background p-4 text-left">
                <p class="text-xs font-black uppercase tracking-[0.16em] text-muted-foreground">Disponible</p>
                <p class="mt-2 text-sm font-bold text-foreground">Descarga directa para Android</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `
})
export class LandingMobileExperienceComponent {
}
