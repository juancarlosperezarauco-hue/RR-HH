import { Component, EventEmitter, inject, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
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
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm overflow-hidden">
      <div
        class="relative flex flex-col w-full max-w-xl max-h-[90vh] rounded-[24px] border border-border bg-card shadow-2xl overflow-hidden">
        
        <!-- Header (Fixed) -->
        <div class="flex items-center justify-between border-b border-border p-6 pb-4 shrink-0 bg-card">
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
            class="inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-border bg-card text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors">
            <lucide-icon name="x" [size]="16"></lucide-icon>
          </button>
        </div>

        <!-- Formulario con Body Scrollable y Footer Fijo -->
        <form (ngSubmit)="submit()" class="flex flex-col flex-1 overflow-hidden">
          <div class="flex-1 overflow-y-auto p-6 space-y-4">
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
                  (ngModelChange)="onShiftTypeChange($event)"
                  name="shiftType"
                  required
                  class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm font-semibold text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary cursor-pointer">
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

            <!-- Sección de Cálculo Automático de Horario y Salida -->
            <div class="rounded-2xl border border-border bg-muted/20 p-4 space-y-3.5">
              <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Configuración de Horario y Salida
                </label>
                <!-- Presets rápidos de jornada -->
                <div class="flex items-center gap-1.5 text-xs">
                  <span class="text-[11px] text-muted-foreground mr-1">Jornada:</span>
                  <button
                    type="button"
                    (click)="setWorkHours(8)"
                    [class.bg-primary]="workHours === 8"
                    [class.text-primary-foreground]="workHours === 8"
                    [class.bg-card]="workHours !== 8"
                    [class.text-foreground]="workHours !== 8"
                    class="rounded-full border border-border px-2.5 py-0.5 text-[11px] font-bold transition-all cursor-pointer hover:border-primary">
                    8h
                  </button>
                  <button
                    type="button"
                    (click)="setWorkHours(7)"
                    [class.bg-primary]="workHours === 7"
                    [class.text-primary-foreground]="workHours === 7"
                    [class.bg-card]="workHours !== 7"
                    [class.text-foreground]="workHours !== 7"
                    class="rounded-full border border-border px-2.5 py-0.5 text-[11px] font-bold transition-all cursor-pointer hover:border-primary">
                    7h
                  </button>
                  <button
                    type="button"
                    (click)="setWorkHours(6)"
                    [class.bg-primary]="workHours === 6"
                    [class.text-primary-foreground]="workHours === 6"
                    [class.bg-card]="workHours !== 6"
                    [class.text-foreground]="workHours !== 6"
                    class="rounded-full border border-border px-2.5 py-0.5 text-[11px] font-bold transition-all cursor-pointer hover:border-primary">
                    6h
                  </button>
                  <button
                    type="button"
                    (click)="setWorkHours(4)"
                    [class.bg-primary]="workHours === 4"
                    [class.text-primary-foreground]="workHours === 4"
                    [class.bg-card]="workHours !== 4"
                    [class.text-foreground]="workHours !== 4"
                    class="rounded-full border border-border px-2.5 py-0.5 text-[11px] font-bold transition-all cursor-pointer hover:border-primary">
                    4h
                  </button>
                </div>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-4 gap-3">
                <!-- Horas de Trabajo (Jornada) -->
                <div>
                  <label class="block text-xs font-bold text-muted-foreground mb-1">
                    Horas Trabajo *
                  </label>
                  <div class="relative">
                    <input
                      type="number"
                      [(ngModel)]="workHours"
                      (ngModelChange)="onWorkHoursChange($event)"
                      name="workHours"
                      min="1"
                      max="12"
                      step="0.5"
                      required
                      class="w-full rounded-xl border border-border bg-card px-3.5 py-2 text-sm font-bold text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
                    <span class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-muted-foreground pointer-events-none">hrs</span>
                  </div>
                </div>

                <!-- Hora Inicio -->
                <div>
                  <label class="block text-xs font-bold text-muted-foreground mb-1">
                    Hora Inicio *
                  </label>
                  <input
                    type="time"
                    [(ngModel)]="startTime"
                    (ngModelChange)="onStartTimeChange($event)"
                    name="startTime"
                    required
                    class="w-full rounded-xl border border-border bg-card px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
                </div>

                <!-- Descanso (minutos) -->
                <div>
                  <label class="block text-xs font-bold text-muted-foreground mb-1">
                    Descanso (min)
                  </label>
                  <div class="relative">
                    <input
                      type="number"
                      [(ngModel)]="breakMinutes"
                      (ngModelChange)="onBreakMinutesChange($event)"
                      name="breakMinutes"
                      min="0"
                      max="180"
                      step="5"
                      class="w-full rounded-xl border border-border bg-card px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
                    <span class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-muted-foreground pointer-events-none">min</span>
                  </div>
                </div>

                <!-- Hora Salida (Auto-calculada) -->
                <div>
                  <div class="flex items-center justify-between mb-1">
                    <label class="block text-xs font-bold text-muted-foreground">
                      Hora Salida *
                    </label>
                    <span class="text-[10px] text-primary font-bold uppercase tracking-wider">Automática</span>
                  </div>
                  <input
                    type="time"
                    [(ngModel)]="endTime"
                    (ngModelChange)="onEndTimeManualChange($event)"
                    name="endTime"
                    required
                    class="w-full rounded-xl border border-primary/50 bg-card px-3.5 py-2 text-sm font-bold text-primary outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
                </div>
              </div>

              <!-- Resumen explicativo de la suma en vivo -->
              <div class="text-[11px] text-muted-foreground flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1.5 bg-card/70 rounded-xl px-3.5 py-2 border border-border/60">
                <span class="flex items-center gap-1.5">
                  <lucide-icon name="clock" [size]="13" class="text-primary shrink-0"></lucide-icon>
                  <span>
                    Inicio <strong>{{ startTime || '--:--' }}</strong> + <strong>{{ workHours }}h</strong> trabajo
                    <ng-container *ngIf="breakMinutes > 0">
                      + <strong>{{ breakMinutes }}m</strong> descanso
                    </ng-container>
                    = Salida <strong>{{ endTime || '--:--' }}</strong>
                  </span>
                </span>
                <span *ngIf="crossesMidnight" class="inline-flex items-center gap-1 rounded-full bg-indigo-500/10 text-indigo-600 px-2.5 py-0.5 text-[10px] font-bold border border-indigo-500/20 w-fit">
                  <lucide-icon name="clock" [size]="10"></lucide-icon>
                  Cruza a día siguiente (+1 día)
                </span>
              </div>
            </div>

            <!-- Tolerancias de Puntualidad (HU16: Entrada y Salida) -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <div class="flex items-center justify-between mb-1.5">
                  <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Tol. Entrada (5 a 15 min) *
                  </label>
                  <span class="text-xs font-bold text-primary">{{ lateToleranceMinutes }} min</span>
                </div>
                <input
                  type="range"
                  [(ngModel)]="lateToleranceMinutes"
                  name="lateToleranceMinutes"
                  min="5"
                  max="15"
                  step="1"
                  class="w-full accent-primary cursor-pointer" />
              </div>

              <div>
                <div class="flex items-center justify-between mb-1.5">
                  <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground">
                    Tol. Salida Anticipada (0 a 15 min)
                  </label>
                  <span class="text-xs font-bold text-primary">{{ earlyDepartureToleranceMinutes }} min</span>
                </div>
                <input
                  type="range"
                  [(ngModel)]="earlyDepartureToleranceMinutes"
                  name="earlyDepartureToleranceMinutes"
                  min="0"
                  max="15"
                  step="1"
                  class="w-full accent-primary cursor-pointer" />
              </div>
            </div>

            <!-- Cruce de Medianoche (Toggle explícito) -->
            <div class="flex items-center justify-between rounded-xl border border-border bg-muted/20 p-3">
              <div class="space-y-0.5">
                <p class="text-xs font-bold text-accent-foreground">Cruza Medianoche (+1 Día)</p>
                <p class="text-[11px] text-muted-foreground">
                  Se activa automáticamente si el horario de salida pasa de las 23:59.
                </p>
              </div>
              <input
                type="checkbox"
                [(ngModel)]="crossesMidnight"
                name="crossesMidnight"
                class="rounded border-border text-primary focus:ring-primary h-4 w-4 cursor-pointer" />
            </div>

            <!-- Panel de Cálculo y Cumplimiento Normativo en Vivo -->
            <div
              [class.border-emerald-500/30]="lawValidation.isValid && !lawValidation.isWarning"
              [class.bg-emerald-500/5]="lawValidation.isValid && !lawValidation.isWarning"
              [class.border-amber-500/40]="lawValidation.isValid && lawValidation.isWarning"
              [class.bg-amber-500/10]="lawValidation.isValid && lawValidation.isWarning"
              [class.border-destructive/30]="!lawValidation.isValid"
              [class.bg-destructive/5]="!lawValidation.isValid"
              class="rounded-2xl border p-4 transition-all space-y-2">
              <div class="flex items-center justify-between">
                <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                  Cálculo de Jornada Efectiva:
                </span>
                <div class="flex items-center gap-2">
                  <span *ngIf="effectiveCalculation.isOvernight || crossesMidnight" class="inline-flex items-center gap-1 rounded-full bg-indigo-500/20 px-2 py-0.5 text-[10px] font-bold text-indigo-700">
                    <lucide-icon name="clock" [size]="10"></lucide-icon>
                    +1 DÍA (CRUCE MEDIANOCHE)
                  </span>
                  <span class="text-sm font-black text-foreground">
                    {{ effectiveCalculation.hours }} hrs
                  </span>
                </div>
              </div>

              <!-- Error bloqueante (> 12h o inconsistencia temporal) -->
              <div *ngIf="!lawValidation.isValid" class="flex items-center gap-2 text-xs font-semibold text-destructive">
                <lucide-icon name="circle-alert" [size]="16" class="shrink-0"></lucide-icon>
                <span>{{ lawValidation.message }}</span>
              </div>

              <!-- Advertencia informativa (> 8h ordinarias o > 7h nocturnas) - Permite guardar -->
              <div *ngIf="lawValidation.isValid && lawValidation.isWarning" class="flex items-center gap-2 text-xs font-semibold text-amber-600 dark:text-amber-400">
                <lucide-icon name="alert-triangle" [size]="16" class="shrink-0"></lucide-icon>
                <span>{{ lawValidation.message }}</span>
              </div>

              <!-- Conforme estándar (<= 8h) -->
              <div *ngIf="lawValidation.isValid && !lawValidation.isWarning" class="flex items-center gap-2 text-xs font-semibold text-emerald-600">
                <lucide-icon name="shield-check" [size]="16" class="shrink-0"></lucide-icon>
                <span>{{ lawValidation.message || 'Jornada ordinaria conforme a la Ley General del Trabajo de Bolivia.' }}</span>
              </div>
            </div>

            <!-- Selector de Días de la Semana -->
            <app-shift-days-picker
              [selectedDays]="workDays"
              (daysChange)="workDays = $event">
            </app-shift-days-picker>
          </div>

          <!-- Botones de Acción (Footer Fijo) -->
          <div class="flex items-center justify-end gap-3 p-6 pt-4 border-t border-border shrink-0 bg-card">
            <button
              type="button"
              (click)="close()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent hover:text-foreground cursor-pointer transition-colors">
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
export class ShiftFormModalComponent implements OnChanges, OnDestroy {
  @Input() isOpen = false;
  @Input() shiftToEdit: ShiftResponse | null = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  readonly formUseCase = inject(ShiftFormUseCase);

  code = '';
  name = '';
  shiftType: ShiftType = 'CONTINUOUS';
  workHours = 8;
  startTime = '08:00';
  endTime = '16:30';
  breakMinutes = 30;
  lateToleranceMinutes = 10;
  earlyDepartureToleranceMinutes = 5;
  crossesMidnight = false;
  workDays: number[] = [1, 2, 3, 4, 5];

  ngOnDestroy(): void {
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']) {
      if (typeof document !== 'undefined') {
        document.body.style.overflow = this.isOpen ? 'hidden' : '';
      }
    }

    if (changes['shiftToEdit'] && this.shiftToEdit) {
      this.code = this.shiftToEdit.code;
      this.name = this.shiftToEdit.name;
      this.shiftType = this.shiftToEdit.shiftType;
      this.startTime = this.shiftToEdit.startTime.substring(0, 5);
      this.endTime = this.shiftToEdit.endTime.substring(0, 5);
      this.breakMinutes = this.shiftToEdit.breakMinutes || 0;
      this.lateToleranceMinutes = this.shiftToEdit.lateToleranceMinutes ?? this.shiftToEdit.gracePeriodMinutes ?? 10;
      this.earlyDepartureToleranceMinutes = this.shiftToEdit.earlyDepartureToleranceMinutes ?? 5;
      this.crossesMidnight = this.shiftToEdit.crossesMidnight ?? this.shiftToEdit.isOvernight ?? false;
      this.workDays = this.shiftToEdit.days
        ? this.shiftToEdit.days.filter((d) => d.isWorkDay).map((d) => d.dayOfWeek)
        : [1, 2, 3, 4, 5];

      const calc = this.formUseCase.calculateEffectiveHours(this.startTime, this.endTime, this.breakMinutes);
      this.workHours = calc.hours || 8;
    } else if (changes['isOpen'] && this.isOpen && !this.shiftToEdit) {
      this.resetForm();
    }
  }

  setWorkHours(hours: number): void {
    this.workHours = hours;
    this.recalculateEndTime();
  }

  onWorkHoursChange(hours: number): void {
    this.workHours = Number(hours) || 0;
    this.recalculateEndTime();
  }

  onStartTimeChange(newTime: string): void {
    this.startTime = newTime;
    this.recalculateEndTime();
  }

  onBreakMinutesChange(mins: number): void {
    this.breakMinutes = Number(mins) || 0;
    this.recalculateEndTime();
  }

  onShiftTypeChange(type: ShiftType): void {
    this.shiftType = type;
    if (type === 'NIGHT' && this.workHours > 7) {
      this.workHours = 7;
    } else if (type !== 'NIGHT' && this.workHours < 8 && this.workHours === 7) {
      this.workHours = 8;
    }
    this.recalculateEndTime();
  }

  onEndTimeManualChange(newEndTime: string): void {
    this.endTime = newEndTime;
    const calc = this.formUseCase.calculateEffectiveHours(this.startTime, this.endTime, this.breakMinutes);
    this.workHours = calc.hours;
    this.crossesMidnight = calc.isOvernight;
  }

  recalculateEndTime(): void {
    if (!this.startTime || this.workHours <= 0) return;
    const [sh, sm] = this.startTime.split(':').map(Number);
    if (isNaN(sh) || isNaN(sm)) return;

    const startMinutes = sh * 60 + sm;
    const durationMinutes = Math.round(this.workHours * 60) + (Number(this.breakMinutes) || 0);
    const totalEndMinutes = startMinutes + durationMinutes;

    const endHour = Math.floor(totalEndMinutes / 60) % 24;
    const endMin = totalEndMinutes % 60;
    this.endTime = `${String(endHour).padStart(2, '0')}:${String(endMin).padStart(2, '0')}`;
    this.crossesMidnight = totalEndMinutes >= 24 * 60 || startMinutes >= (endHour * 60 + endMin);
  }

  get effectiveCalculation() {
    const calc = this.formUseCase.calculateEffectiveHours(this.startTime, this.endTime, this.breakMinutes);
    return calc;
  }

  get lawValidation() {
    return this.formUseCase.validateLaborLaw({
      code: this.code,
      name: this.name,
      shiftType: this.shiftType,
      startTime: this.startTime,
      endTime: this.endTime,
      breakMinutes: this.breakMinutes,
      lateToleranceMinutes: this.lateToleranceMinutes,
      earlyDepartureToleranceMinutes: this.earlyDepartureToleranceMinutes,
      gracePeriodMinutes: this.lateToleranceMinutes,
      crossesMidnight: this.crossesMidnight,
      isOvernight: this.crossesMidnight || this.effectiveCalculation.isOvernight,
      workDays: this.workDays
    });
  }

  isValid(): boolean {
    return !!this.code && !!this.name && this.workHours > 0 && !!this.startTime && !!this.endTime && this.lawValidation.isValid;
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
        lateToleranceMinutes: this.lateToleranceMinutes,
        earlyDepartureToleranceMinutes: this.earlyDepartureToleranceMinutes,
        gracePeriodMinutes: this.lateToleranceMinutes,
        crossesMidnight: this.crossesMidnight,
        isOvernight: this.crossesMidnight || this.effectiveCalculation.isOvernight,
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
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
    this.closeModal.emit();
  }

  private resetForm(): void {
    this.code = '';
    this.name = '';
    this.shiftType = 'CONTINUOUS';
    this.workHours = 8;
    this.startTime = '08:00';
    this.breakMinutes = 30;
    this.lateToleranceMinutes = 10;
    this.earlyDepartureToleranceMinutes = 5;
    this.workDays = [1, 2, 3, 4, 5];
    this.recalculateEndTime();
  }
}

