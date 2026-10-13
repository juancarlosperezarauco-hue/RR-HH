import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ShiftResponse, ShiftType } from '../../../../entities/shift';
import { ShiftFormUseCase } from '../../application/shift-form.usecase';
import { ShiftDaysPickerComponent } from '../shift-days-picker/shift-days-picker.component';

@Component({
  selector: 'app-shift-form-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, ShiftDaysPickerComponent],
  template: `
    <div
      *ngIf="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm overflow-y-auto">
      <div
        class="relative w-full max-w-xl rounded-[24px] border border-border bg-card p-6 shadow-2xl space-y-5 my-8">
        
        <!-- Header -->
        <div class="flex items-center justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-xl font-bold text-accent-foreground">
              {{ shiftToEdit ? 'Editar Turno Laboral' : 'Nuevo Turno de Trabajo' }}
            </h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Parametrización conforme a la Ley General del Trabajo (LGT Bolivia).
            </p>
          </div>
          <button
            type="button"
            (click)="close()"
            class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground hover:bg-destructive/10 hover:text-destructive">
            <lucide-icon name="x" [size]="16"></lucide-icon>
          </button>
        </div>

        <!-- Formulario -->
        <form (ngSubmit)="submit()" class="space-y-4">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <!-- Código -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Código del Turno *
              </label>
              <input
                type="text"
                [(ngModel)]="code"
                name="code"
                required
                placeholder="Ej: TUR_DIURNO_OFI"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm font-semibold text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>

            <!-- Tipo de Turno -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Régimen / Tipo *
              </label>
              <select
                [(ngModel)]="shiftType"
                name="shiftType"
                required
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm font-semibold text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                <option value="CONTINUOUS">Continuo (Máx 8h)</option>
                <option value="NIGHT">Nocturno (Máx 7h)</option>
                <option value="ROTATING">Rotativo</option>
                <option value="SPLIT">Discontinuo</option>
              </select>
            </div>
          </div>

          <!-- Nombre descriptivo -->
          <div>
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Nombre Descriptivo *
            </label>
            <input
              type="text"
              [(ngModel)]="name"
              name="name"
              required
              placeholder="Ej: Turno Administrativo Central (8 horas)"
              class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
          </div>

          <!-- Horarios: Inicio, Fin y Descanso -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Hora Inicio *
              </label>
              <input
                type="time"
                [(ngModel)]="startTime"
                name="startTime"
                required
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>

            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Hora Fin *
              </label>
              <input
                type="time"
                [(ngModel)]="endTime"
                name="endTime"
                required
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>

            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Descanso (min)
              </label>
              <input
                type="number"
                [(ngModel)]="breakMinutes"
                name="breakMinutes"
                min="0"
                step="5"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>
          </div>

          <!-- Tolerancia de puntualidad (5 a 15 min) -->
          <div>
            <div class="flex items-center justify-between mb-1.5">
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                Tolerancia de Puntualidad (LGT: 5 a 15 min) *
              </label>
              <span class="text-xs font-bold text-primary">{{ gracePeriodMinutes }} minutos</span>
            </div>
            <input
              type="range"
              [(ngModel)]="gracePeriodMinutes"
              name="gracePeriodMinutes"
              min="5"
              max="15"
              step="1"
              class="w-full accent-primary cursor-pointer" />
          </div>

          <!-- Panel de Cálculo y Cumplimiento Normativo en Vivo -->
          <div
            [class.border-emerald-500/30]="lawValidation.isValid"
            [class.bg-emerald-500/5]="lawValidation.isValid"
            [class.border-destructive/30]="!lawValidation.isValid"
            [class.bg-destructive/5]="!lawValidation.isValid"
            class="rounded-2xl border p-4 transition-all space-y-2">
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                Cálculo de Jornada Efectiva:
              </span>
              <div class="flex items-center gap-2">
                <span *ngIf="effectiveCalculation.isOvernight" class="inline-flex items-center gap-1 rounded-full bg-indigo-500/20 px-2 py-0.5 text-[10px] font-bold text-indigo-700">
                  <lucide-icon name="clock" [size]="10"></lucide-icon>
                  +1 DÍA (CRUCE MEDIANOCHE)
                </span>
                <span class="text-sm font-black text-foreground">
                  {{ effectiveCalculation.hours }} hrs
                </span>
              </div>
            </div>

            <div *ngIf="!lawValidation.isValid" class="flex items-center gap-2 text-xs font-semibold text-destructive">
              <lucide-icon name="circle-alert" [size]="16" class="shrink-0"></lucide-icon>
              <span>{{ lawValidation.message }}</span>
            </div>

            <div *ngIf="lawValidation.isValid" class="flex items-center gap-2 text-xs font-semibold text-emerald-600">
              <lucide-icon name="shield-check" [size]="16" class="shrink-0"></lucide-icon>
              <span>Jornada conforme a la Ley General del Trabajo de Bolivia.</span>
            </div>
          </div>

          <!-- Selector de Días de la Semana -->
          <app-shift-days-picker
            [selectedDays]="workDays"
            (daysChange)="workDays = $event">
          </app-shift-days-picker>

          <!-- Botones de Acción -->
          <div class="flex items-center justify-end gap-3 pt-3 border-t border-border">
            <button
              type="button"
              (click)="close()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent hover:text-foreground cursor-pointer">
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="!isValid() || formUseCase.saving()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-primary px-5 py-2 text-xs font-semibold text-primary-foreground shadow-md transition-all hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed">
              <lucide-icon
                *ngIf="formUseCase.saving()"
                name="loader-circle"
                [size]="14"
                class="animate-spin"></lucide-icon>
              {{ formUseCase.saving() ? 'Guardando...' : (shiftToEdit ? 'Actualizar Turno' : 'Crear Turno') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class ShiftFormModalComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() shiftToEdit: ShiftResponse | null = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  readonly formUseCase = inject(ShiftFormUseCase);

  code = '';
  name = '';
  shiftType: ShiftType = 'CONTINUOUS';
  startTime = '08:30';
  endTime = '16:30';
  breakMinutes = 45;
  gracePeriodMinutes = 10;
  workDays: number[] = [1, 2, 3, 4, 5];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['shiftToEdit'] && this.shiftToEdit) {
      this.code = this.shiftToEdit.code;
      this.name = this.shiftToEdit.name;
      this.shiftType = this.shiftToEdit.shiftType;
      this.startTime = this.shiftToEdit.startTime.substring(0, 5);
      this.endTime = this.shiftToEdit.endTime.substring(0, 5);
      this.breakMinutes = this.shiftToEdit.breakMinutes;
      this.gracePeriodMinutes = this.shiftToEdit.gracePeriodMinutes;
      this.workDays = this.shiftToEdit.days
        ? this.shiftToEdit.days.filter((d) => d.isWorkDay).map((d) => d.dayOfWeek)
        : [1, 2, 3, 4, 5];
    } else if (changes['isOpen'] && this.isOpen && !this.shiftToEdit) {
      this.resetForm();
    }
  }

  get effectiveCalculation() {
    return this.formUseCase.calculateEffectiveHours(this.startTime, this.endTime, this.breakMinutes);
  }

  get lawValidation() {
    return this.formUseCase.validateLaborLaw({
      code: this.code,
      name: this.name,
      shiftType: this.shiftType,
      startTime: this.startTime,
      endTime: this.endTime,
      breakMinutes: this.breakMinutes,
      gracePeriodMinutes: this.gracePeriodMinutes,
      isOvernight: this.effectiveCalculation.isOvernight,
      workDays: this.workDays
    });
  }

  isValid(): boolean {
    return !!this.code && !!this.name && this.lawValidation.isValid;
  }

  async submit(): Promise<void> {
    if (!this.isValid()) return;

    const ok = await this.formUseCase.saveShift(
      {
        code: this.code,
        name: this.name,
        shiftType: this.shiftType,
        startTime: this.startTime.length === 5 ? `${this.startTime}:00` : this.startTime,
        endTime: this.endTime.length === 5 ? `${this.endTime}:00` : this.endTime,
        breakMinutes: this.breakMinutes,
        gracePeriodMinutes: this.gracePeriodMinutes,
        isOvernight: this.effectiveCalculation.isOvernight,
        workDays: this.workDays
      },
      this.shiftToEdit?.id
    );

    if (ok) {
      this.saved.emit();
      this.close();
    }
  }

  close(): void {
    this.closeModal.emit();
  }

  private resetForm(): void {
    this.code = '';
    this.name = '';
    this.shiftType = 'CONTINUOUS';
    this.startTime = '08:30';
    this.endTime = '16:30';
    this.breakMinutes = 45;
    this.gracePeriodMinutes = 10;
    this.workDays = [1, 2, 3, 4, 5];
  }
}
