import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

interface LandingBenefitCard {
  icon: string;
  title: string;
  description: string;
  accent: 'primary' | 'secondary';
  bullets: string[];
}

@Component({
  selector: 'app-landing-benefits',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section id="beneficios" class="border-t border-border bg-card px-4 py-20 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-7xl">
        <div class="mx-auto max-w-3xl text-center">
          <div class="mx-auto mb-5 inline-flex w-fit items-center gap-2 rounded-full bg-accent px-5 py-2 text-sm font-black text-primary">
            <lucide-icon name="shield-check" class="h-4 w-4"></lucide-icon>
            Beneficios
          </div>

          <h2 class="text-4xl font-black tracking-tight text-foreground sm:text-5xl">
            Gestión humana más simple y ordenada
          </h2>

          <p class="mt-5 text-base leading-8 text-muted-foreground">
            Una experiencia limpia para administrar tenants, usuarios, seguridad y suscripciones desde un solo lugar.
          </p>
        </div>

        <div class="mt-12 grid gap-6 md:grid-cols-2 xl:grid-cols-4">
          @for (card of cards; track card.title) {
            <article class="rounded-[28px] border border-border bg-muted p-7 shadow-sm">
              <div
                class="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl"
                [class.bg-accent]="card.accent === 'primary'"
                [class.text-primary]="card.accent === 'primary'"
                [class.bg-accent]="card.accent === 'secondary'"
                [class.text-info]="card.accent === 'secondary'">
                <lucide-icon [name]="card.icon" class="h-5 w-5"></lucide-icon>
              </div>

              <h3 class="text-lg font-black text-foreground">{{ card.title }}</h3>
              <p class="mt-3 text-sm leading-7 text-muted-foreground">
                {{ card.description }}
              </p>

              <ul class="mt-5 space-y-3 text-sm text-muted-foreground">
                @for (bullet of card.bullets; track bullet) {
                  <li class="flex items-start gap-2">
                    <lucide-icon name="check-circle" class="mt-0.5 h-4 w-4 shrink-0 text-primary"></lucide-icon>
                    <span>{{ bullet }}</span>
                  </li>
                }
              </ul>
            </article>
          }
        </div>
      </div>
    </section>
  `
})
export class LandingBenefitsComponent {
  readonly cards: LandingBenefitCard[] = [
    {
      icon: 'users',
      title: 'Personas y organizaciones',
      description: 'Administra organizaciones, usuarios, roles y planes desde un inicio claro y sin duplicar flujos.',
      accent: 'primary',
      bullets: ['Multi-tenant', 'Roles claros', 'Configuración por plan']
    },
    {
      icon: 'users',
      title: 'Personas y equipos',
      description: 'Centraliza colaboradores, equipos y perfiles con una experiencia clara para usuarios y administradores.',
      accent: 'secondary',
      bullets: ['Directorio de colaboradores', 'Equipos organizados', 'Perfiles por rol', 'Información centralizada']
    },
    {
      icon: 'shield-check',
      title: 'Seguridad y cumplimiento',
      description: 'Controla el acceso con roles, permisos y sesiones seguras para proteger la información de tu organización.',
      accent: 'primary',
      bullets: ['RBAC', 'Permisos granulares', 'Auditoría', 'Sesiones seguras']
    },
  ];
}
