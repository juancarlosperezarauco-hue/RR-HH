import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-platform-pagination',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="flex flex-col gap-3 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
      <div class="text-sm text-[#567157]">
        <span class="font-semibold text-[#1B5E20]">{{ totalElements }}</span>
        registro(s)
        <span class="mx-1">·</span>
        Página
        <span class="font-semibold text-[#1B5E20]">{{ currentPageLabel }}</span>
        de
        <span class="font-semibold text-[#1B5E20]">{{ totalPagesLabel }}</span>
      </div>

      <div class="flex items-center gap-2">
        <button
          type="button"
          (click)="goToPage(currentPage - 1)"
          [disabled]="isLoading || currentPage <= 0"
          class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-3 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9] disabled:cursor-not-allowed disabled:opacity-50">
          <lucide-icon name="arrow-left" class="h-4 w-4"></lucide-icon>
          Anterior
        </button>

        <button
          type="button"
          (click)="goToPage(currentPage + 1)"
          [disabled]="isLoading || currentPage + 1 >= totalPages"
          class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-3 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9] disabled:cursor-not-allowed disabled:opacity-50">
          Siguiente
          <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
        </button>
      </div>
    </div>
  `
})
export class PlatformPaginationComponent {
  @Input() currentPage = 0;
  @Input() totalPages = 0;
  @Input() totalElements = 0;
  @Input() isLoading = false;

  @Output() pageChange = new EventEmitter<number>();

  get currentPageLabel(): number {
    return this.totalPages === 0 ? 0 : this.currentPage + 1;
  }

  get totalPagesLabel(): number {
    return this.totalPages === 0 ? 0 : this.totalPages;
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || this.isLoading) {
      return;
    }

    this.pageChange.emit(page);
  }
}
