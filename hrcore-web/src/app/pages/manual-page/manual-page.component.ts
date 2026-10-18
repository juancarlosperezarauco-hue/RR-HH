import { Component, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { PublicFooterComponent, PublicNavbarComponent } from '../../shared/ui/public-layout';
import { MANUAL_SECTIONS, ManualSection } from './manual-content';
import { ManualSectionComponent } from './manual-section.component';
import { ManualSidebarComponent } from './manual-sidebar.component';

@Component({
  selector: 'app-manual-page',
  standalone: true,
  imports: [RouterModule, PublicNavbarComponent, PublicFooterComponent, ManualSidebarComponent, ManualSectionComponent],
  template: `
    <div class="hrcore-brand-theme flex min-h-screen flex-col bg-background">
      <app-public-navbar />

      <main class="flex-1 px-4 py-10 sm:px-6 lg:px-8 lg:py-14">
        <div class="mx-auto max-w-7xl">
          <header class="max-w-3xl">
            <p class="text-xs font-black uppercase tracking-[0.2em] text-primary">Guía de uso</p>
            <h1 class="mt-3 text-3xl font-black tracking-tight text-accent-foreground sm:text-5xl">Manual de HR Core</h1>
            <p class="mt-4 text-base leading-8 text-muted-foreground sm:text-lg">
              Aprende a configurar tu organización, gestionar personas y operar los procesos principales de recursos humanos.
            </p>
            <div class="mt-6 flex flex-wrap gap-2 text-xs font-bold text-muted-foreground">
              <span class="rounded-full border border-border bg-card px-3 py-1.5">Guía por módulos</span>
              <span class="rounded-full border border-border bg-card px-3 py-1.5">Actualizada para HR Core</span>
              <span class="rounded-full border border-border bg-card px-3 py-1.5">12 secciones</span>
            </div>
          </header>

          <div class="mt-10 grid gap-6 lg:grid-cols-[250px_minmax(0,1fr)] lg:items-start">
            <app-manual-sidebar
              [sections]="sections"
              [activeId]="activeId()"
              (sectionSelected)="selectSection($event)"
            />

            <div class="min-w-0">
              <app-manual-section [section]="activeSection()" />
              <div class="mt-4 flex justify-end">
                <a routerLink="/" class="text-sm font-bold text-primary transition-colors hover:text-primary/80 hover:underline">Volver al inicio</a>
              </div>
            </div>
          </div>
        </div>
      </main>

      <app-public-footer />
    </div>
  `,
})
export class ManualPageComponent {
  readonly sections = MANUAL_SECTIONS;
  readonly activeId = signal(this.sections[0].id);
  readonly activeSection = signal<ManualSection>(this.sections[0]);

  selectSection(id: string): void {
    const section = this.sections.find((item) => item.id === id);
    if (!section) return;
    this.activeId.set(section.id);
    this.activeSection.set(section);
  }
}
