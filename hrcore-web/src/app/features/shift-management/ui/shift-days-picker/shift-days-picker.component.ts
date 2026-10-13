import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface DayOption {
  dayOfWeek: number;
  label: string;
  short: string;
}

@Component({
  selector: 'app-shift-days-picker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-2.5">
      <div class="flex items-center justify-between">
        <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
          Días Laborales de la Jornada *
        </label>
        <!-- Presets rápidos -->
        <div class="flex gap-2 text-[11px]">
          <button
            type="button"
            (click)="setPreset([1, 2, 3, 4, 5])"
            class="text-primary hover:underline font-semibold cursor-pointer">
            L-V
          </button>
          <span class="text-muted-foreground">•</span>
          <button
            type="button"
            (click)="setPreset([1, 2, 3, 4, 5, 6])"
            class="text-primary hover:underline font-semibold cursor-pointer">
            L-S
          </button>
          <span class="text-muted-foreground">•</span>
          <button
            type="button"
            (click)="setPreset([1, 2, 3, 4, 5, 6, 7])"
            class="text-primary hover:underline font-semibold cursor-pointer">
            Todos
          </button>
        </div>
      </div>

      <div class="grid grid-cols-7 gap-1.5 sm:gap-2">
        <button
          *ngFor="let d of days"
          type="button"
          (click)="toggleDay(d.dayOfWeek)"
          [class.bg-primary]="isSelected(d.dayOfWeek)"
          [class.text-primary-foreground]="isSelected(d.dayOfWeek)"
          [class.shadow-sm]="isSelected(d.dayOfWeek)"
          [class.bg-card]="!isSelected(d.dayOfWeek)"
          [class.text-muted-foreground]="!isSelected(d.dayOfWeek)"
          class="flex flex-col items-center justify-center rounded-xl border border-border py-2 text-xs font-bold transition-all hover:border-primary/50 cursor-pointer">
          <span>{{ d.short }}</span>
        </button>
      </div>

      <p class="text-[11px] text-muted-foreground">
        {{ selectedDays.length }} días seleccionados por semana
      </p>
    </div>
  `
})
export class ShiftDaysPickerComponent {
  @Input() selectedDays: number[] = [1, 2, 3, 4, 5];
  @Output() daysChange = new EventEmitter<number[]>();

  readonly days: DayOption[] = [
    { dayOfWeek: 1, label: 'Lunes', short: 'Lun' },
    { dayOfWeek: 2, label: 'Martes', short: 'Mar' },
    { dayOfWeek: 3, label: 'Miércoles', short: 'Mié' },
    { dayOfWeek: 4, label: 'Jueves', short: 'Jue' },
    { dayOfWeek: 5, label: 'Viernes', short: 'Vie' },
    { dayOfWeek: 6, label: 'Sábado', short: 'Sáb' },
    { dayOfWeek: 7, label: 'Domingo', short: 'Dom' }
  ];

  isSelected(day: number): boolean {
    return this.selectedDays.includes(day);
  }

  toggleDay(day: number): void {
    let updated: number[];
    if (this.isSelected(day)) {
      updated = this.selectedDays.filter((d) => d !== day);
    } else {
      updated = [...this.selectedDays, day].sort((a, b) => a - b);
    }
    this.daysChange.emit(updated);
  }

  setPreset(days: number[]): void {
    this.daysChange.emit(days);
  }
}
