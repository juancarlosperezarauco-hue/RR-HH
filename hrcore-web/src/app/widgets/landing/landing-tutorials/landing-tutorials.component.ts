import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { LucideAngularModule } from 'lucide-angular';

interface LandingTutorialVideo {
  icon: string;
  title: string;
  description: string;
  url: string;
  badge: string;
  safeUrl: SafeResourceUrl;
}

@Component({
  selector: 'app-landing-tutorials',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section id="tutoriales" class="relative isolate overflow-hidden bg-[#F3FAF1] px-4 py-20 sm:px-6 lg:px-8">
      <div class="absolute inset-0 -z-10 opacity-[0.22] [background-image:radial-gradient(#2E7D32_1px,transparent_1px)] [background-size:30px_30px]"></div>

      <div class="mx-auto max-w-7xl">
        <div class="mx-auto max-w-3xl text-center">
          <div class="mx-auto mb-5 inline-flex w-fit items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-5 py-2 text-sm font-black text-[#2E7D32] shadow-sm">
            <lucide-icon name="play" class="h-4 w-4"></lucide-icon>
            Tutoriales rápidos
          </div>

          <h2 class="text-4xl font-black tracking-tight text-[#101827] sm:text-5xl">
            Aprende a usar la plataforma con videos cortos
          </h2>

          <p class="mt-5 text-base leading-8 text-[#405447]">
            Mira los flujos principales de acceso y administración de personas antes de entrar al sistema.
          </p>
        </div>

        <div class="mt-14 grid gap-7 lg:grid-cols-2">
          @for (video of videos; track video.title) {
            <article class="overflow-hidden rounded-[30px] border border-[#DDEED8] bg-white shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
              <div class="aspect-video bg-[#102016]">
                <iframe
                  class="h-full w-full"
                  [src]="video.safeUrl"
                  [title]="video.title"
                  frameborder="0"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  allowfullscreen>
                </iframe>
              </div>

              <div class="p-6">
                <span class="inline-flex rounded-full bg-[#E8F5E9] px-4 py-1.5 text-xs font-black uppercase tracking-[0.16em] text-[#2E7D32]">
                  {{ video.badge }}
                </span>

                <h3 class="mt-4 text-2xl font-black text-[#101827]">
                  {{ video.title }}
                </h3>

                <p class="mt-3 text-sm leading-7 text-[#405447]">
                  {{ video.description }}
                </p>
              </div>
            </article>
          }
        </div>

        <div class="mt-10 flex justify-center">
          <a
            [href]="youtubeChannelUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#2E7D32] px-5 text-sm font-black text-white shadow-sm transition-colors hover:bg-[#256428]">
            Ir al canal de YouTube
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>
        </div>
      </div>
    </section>
  `
})
export class LandingTutorialsComponent {
  readonly youtubeChannelUrl = 'https://youtube.com/@prospera-6777?si=Bb9wP5j311yJgtKF';
  readonly videos: LandingTutorialVideo[];

  constructor(private readonly sanitizer: DomSanitizer) {
    this.videos = [
      {
        icon: 'log-in',
        title: 'Cómo iniciar sesión con face-recognition',
        description: 'Aprende cómo ingresar con rostro usando la foto de perfil y el flujo de acceso facial del sistema.',
        url: 'https://www.youtube.com/embed/RgVVV59qzMI?si=2qieICAG3MGf8-VC',
        badge: 'Acceso con rostro',
        safeUrl: this.sanitizer.bypassSecurityTrustResourceUrl('https://www.youtube.com/embed/RgVVV59qzMI?si=2qieICAG3MGf8-VC')
      },
      {
        icon: 'credit-card',
        title: 'Cómo administrar usuarios y roles',
        description: 'Mira cómo organizar usuarios y permisos desde la experiencia del tenant, paso a paso.',
        url: 'https://www.youtube.com/embed/Q907PF4RQXU?si=FMCvZAtQ_IRFxTUC',
        badge: 'Usuarios y roles',
        safeUrl: this.sanitizer.bypassSecurityTrustResourceUrl('https://www.youtube.com/embed/Q907PF4RQXU?si=FMCvZAtQ_IRFxTUC')
      }
    ];
  }
}
