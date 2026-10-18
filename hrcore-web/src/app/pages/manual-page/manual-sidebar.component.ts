import { Component, input, output } from '@angular/core';
import { ManualSection } from './manual-content';

@Component({
  selector: 'app-manual-sidebar',
  standalone: true,
  template: `
    <aside class="rounded-[24px] border border-border bg-card p-3 shadow-sm lg:sticky lg:top-24">
      <div class="px-3 pb-3 pt-2">
        <p class="text-xs font-black uppercase tracking-[0.18em] text-muted-foreground">Contenido</p>
        <p class="mt-1 text-sm text-muted-foreground">Navega por las tareas principales.</p>
      </div>
      <nav aria-label="Secciones del manual" class="grid gap-1 sm:grid-cols-2 lg:grid-cols-1">
        @for (section of sections(); track section.id) {
          <button
            type="button"
            class="rounded-2xl px-3 py-2.5 text-left text-sm font-bold transition-colors"
            [class.bg-primary]="activeId() === section.id"
            [class.text-primary-foreground]="activeId() === section.id"
            [class.text-accent-foreground]="activeId() !== section.id"
            [class.hover:bg-muted]="activeId() !== section.id"
            [attr.aria-current]="activeId() === section.id ? 'page' : null"
            (click)="sectionSelected.emit(section.id)"
          >
            {{ section.label }}
          </button>
        }
      </nav>
    </aside>
  `
})
export class ManualSidebarComponent {
  readonly sections = input.required<ManualSection[]>();
  readonly activeId = input.required<string>();
  readonly sectionSelected = output<string>();
}
