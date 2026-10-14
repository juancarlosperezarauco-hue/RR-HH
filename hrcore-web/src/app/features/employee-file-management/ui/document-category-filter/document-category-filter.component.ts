import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface CategoryOption {
  key: string;
  label: string;
}

@Component({
  selector: 'app-document-category-filter',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-wrap items-center gap-2 border-b border-border pb-3">
      <button
        *ngFor="let cat of categories"
        type="button"
        (click)="selectCategory(cat.key)"
        [class.bg-primary]="activeCategory === cat.key"
        [class.text-primary-foreground]="activeCategory === cat.key"
        [class.shadow-sm]="activeCategory === cat.key"
        [class.bg-card]="activeCategory !== cat.key"
        [class.text-muted-foreground]="activeCategory !== cat.key"
        class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-border px-3.5 py-1.5 text-xs font-semibold transition-all hover:bg-accent hover:text-foreground">
        {{ cat.label }}
      </button>
    </div>
  `
})
export class DocumentCategoryFilterComponent {
  @Input() activeCategory = 'ALL';
  @Output() categoryChange = new EventEmitter<string>();

  readonly categories: CategoryOption[] = [
    { key: 'ALL', label: 'Todos' },
    { key: 'IDENTIFICATION', label: 'Identificación' },
    { key: 'CONTRACT', label: 'Contratos' },
    { key: 'EDUCATION', label: 'Académicos' },
    { key: 'HEALTH', label: 'Médicos (Sensible)' },
    { key: 'DISCIPLINARY', label: 'Disciplinarios' },
    { key: 'ASSET', label: 'Equipos y Activos' }
  ];

  selectCategory(key: string): void {
    if (this.activeCategory !== key) {
      this.categoryChange.emit(key);
    }
  }
}
