import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

interface MobileScreenshot {
  title: string;
  description: string;
  imageUrl: string;
}

@Component({
  selector: 'app-landing-mobile-screenshots',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="bg-card px-4 pb-6 pt-10 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-7xl">
        <div class="grid gap-6 sm:grid-cols-3">
          @for (shot of screenshots; track shot.title) {
            <article class="text-center">
              <div class="mx-auto flex h-[500px] w-full max-w-[245px] flex-col rounded-[34px] border-[8px] border-border bg-sidebar shadow-lg">
                <div class="mx-auto mb-2 mt-1 h-5 w-20 rounded-full bg-sidebar"></div>
                <div class="flex flex-1 flex-col overflow-hidden rounded-[24px] bg-card">
                  <img [src]="shot.imageUrl" [alt]="shot.title" class="h-full w-full object-cover" />
                </div>
              </div>

              <h3 class="mt-5 font-black text-foreground">{{ shot.title }}</h3>
              <p class="mx-auto mt-2 max-w-[230px] text-sm leading-6 text-muted-foreground">
                {{ shot.description }}
              </p>
            </article>
          }
        </div>
      </div>
    </section>
  `
})
export class LandingMobileScreenshotsComponent {
  readonly screenshots: MobileScreenshot[] = [
    {
      title: 'Login',
      description: 'Acceso rápido y seguro desde el teléfono.',
      imageUrl: '/landing/mobile-login.png'
    },
    {
      title: 'Inicio',
      description: 'Resumen claro de personas y acciones principales.',
      imageUrl: '/landing/mobile-home.png'
    },
    {
      title: 'Equipo',
      description: 'Consulta perfiles y equipos en pocos pasos.',
      imageUrl: '/landing/mobile-payments.png'
    }
  ];
}
