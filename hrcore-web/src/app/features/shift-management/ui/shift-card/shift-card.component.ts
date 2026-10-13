import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { ShiftResponse } from '../../../../entities/shift';
import { ShiftStatusBadgeComponent } from '../shift-status-badge/shift-status-badge.component';
import { ShiftFormUseCase } from '../../application/shift-form.usecase';

@Component({
  selector: 'app-shift-card',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ShiftStatusBadgeComponent],
  template: `
    <div class="group relative flex flex-col justify-between rounded-2xl border border-border bg-card p-5 shadow-sm transition-all hover:shadow-md hover:border-primary/40">
      <div class="space-y-3">
        <!-- Encabezado de la Tarjeta -->
        <div class="flex items-start justify-between gap-2">
          <div class="space-y-1">
            <span
              [class]="getTypeBadgeClass(shift.shiftType)"
              class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider">
              {{ formatType(shift.shiftType) }}
            </span>
            <span *ngIf="shift.isOvernight" class="ml-2 inline-flex items-center gap-1 rounded-full bg-indigo-500/10 px-2 py-0.5 text-[10px] font-bold text-indigo-600 border border-indigo-500/20">
              <lucide-icon name="clock" [size]="10"></lucide-icon>
              +1 DÍA (NOCTURNO)
            </span>
          </div>

          <app-shift-status-badge [status]="shift.status"></app-shift-status-badge>
        </div>

        <div>
          <h4 class="text-base font-bold text-accent-foreground group-hover:text-primary transition-colors">
            {{ shift.name }}
          </h4>
          <p class="text-xs font-mono font-semibold text-muted-foreground mt-0.5">
            Código: <span class="text-foreground">{{ shift.code }}</span>
          </p>
        </div>
      </div>

      <!-- Horario y Horas Efectivas Computadas -->
      <div class="my-4 space-y-2 border-t border-b border-border/60 py-3 text-xs">
        <div class="flex items-center justify-between">
          <span class="text-muted-foreground font-medium">Horario:</span>
          <span class="font-bold text-foreground flex items-center gap-1.5">
            <lucide-icon name="clock" [size]="13" class="text-primary"></lucide-icon>
            {{ formatTime(shift.startTime) }} - {{ formatTime(shift.endTime) }}
          </span>
        </div>

        <div class="flex items-center justify-between">
          <span class="text-muted-foreground font-medium">Jornada Efectiva:</span>
          <span class="font-bold text-emerald-600 bg-emerald-500/10 px-2 py-0.5 rounded-md">
            {{ effectiveHours }}h / día (LGT)
          </span>
        </div>

        <div class="flex items-center justify-between">
          <span class="text-muted-foreground font-medium">Descanso / Tolerancia:</span>
          <span class="font-medium text-foreground">
            {{ shift.breakMinutes }}m desc. • {{ shift.gracePeriodMinutes }}m tol.
          </span>
        </div>

        <!-- Días Laborales -->
        <div class="pt-1">
          <span class="text-[11px] text-muted-foreground block mb-1">Días de aplicación:</span>
          <div class="flex gap-1">
            <span
              *ngFor="let d of weekDays"
              [class.bg-primary]="isWorkDay(d.id)"
              [class.text-primary-foreground]="isWorkDay(d.id)"
              [class.bg-muted]="!isWorkDay(d.id)"
              [class.text-muted-foreground]="!isWorkDay(d.id)"
              class="flex h-5 w-5 items-center justify-center rounded-md text-[10px] font-bold">
              {{ d.label }}
            </span>
          </div>
        </div>
      </div>

      <!-- Acciones Inferiores -->
      <div class="flex items-center justify-between gap-2 pt-1 border-t border-border/40">
        <button
          type="button"
          (click)="assign.emit(shift)"
          class="inline-flex flex-1 cursor-pointer items-center justify-center gap-1.5 rounded-full bg-primary/10 py-1.5 text-xs font-semibold text-primary transition-colors hover:bg-primary hover:text-primary-foreground">
          <lucide-icon name="users" [size]="14"></lucide-icon>
          Asignar
        </button>

        <button
          type="button"
          (click)="edit.emit(shift)"
          class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
          title="Editar turno">
          <lucide-icon name="pencil" [size]="14"></lucide-icon>
        </button>

        <button
          type="button"
          (click)="toggleStatus.emit(shift)"
          [title]="shift.status === 'ACTIVE' ? 'Desactivar turno' : 'Activar turno'"
          class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive">
          <lucide-icon [name]="shift.status === 'ACTIVE' ? 'ban' : 'check-circle'" [size]="14"></lucide-icon>
        </button>
      </div>
    </div>
  `
})
export class ShiftCardComponent {
  @Input({ required: true }) shift!: ShiftResponse;
  @Output() edit = new EventEmitter<ShiftResponse>();
  @Output() assign = new EventEmitter<ShiftResponse>();
  @Output() toggleStatus = new EventEmitter<ShiftResponse>();

  private readonly formUseCase = inject(ShiftFormUseCase);

  readonly weekDays = [
    { id: 1, label: 'L' },
    { id: 2, label: 'M' },
    { id: 3, label: 'M' },
    { id: 4, label: 'J' },
    { id: 5, label: 'V' },
    { id: 6, label: 'S' },
    { id: 7, label: 'D' }
  ];

  get effectiveHours(): number {
    return this.formUseCase.calculateEffectiveHours(
      this.shift.startTime,
      this.shift.endTime,
      this.shift.breakMinutes
    ).hours;
  }

  isWorkDay(dayOfWeek: number): boolean {
    if (!this.shift.days) return dayOfWeek <= 5;
    return this.shift.days.some((d) => d.dayOfWeek === dayOfWeek && d.isWorkDay);
  }

  formatTime(timeStr: string): string {
    if (!timeStr) return '';
    return timeStr.substring(0, 5); // '08:30:00' -> '08:30'
  }

  formatType(type: string): string {
    switch (type) {
      case 'CONTINUOUS': return 'Continuo';
      case 'NIGHT': return 'Nocturno';
      case 'ROTATING': return 'Rotativo';
      case 'SPLIT': return 'Discontinuo';
      default: return type;
    }
  }

  getTypeBadgeClass(type: string): string {
    switch (type) {
      case 'CONTINUOUS': return 'bg-blue-500/10 text-blue-600 border border-blue-500/20';
      case 'NIGHT': return 'bg-indigo-500/10 text-indigo-600 border border-indigo-500/20';
      case 'ROTATING': return 'bg-amber-500/10 text-amber-600 border border-amber-500/20';
      case 'SPLIT': return 'bg-emerald-500/10 text-emerald-600 border border-emerald-500/20';
      default: return 'bg-muted text-muted-foreground border border-border';
    }
  }
}
