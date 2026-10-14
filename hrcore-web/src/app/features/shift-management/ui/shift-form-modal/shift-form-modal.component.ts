import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ShiftResponse, ShiftType } from '../../../../entities/shift';
import { ShiftFormUseCase } from '../../application/shift-form.usecase';

@Component({
  selector: 'app-shift-form-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
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

            <!-- Tipo de Turno derivado del horario -->
            <div>
              <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
                Régimen calculado
              </label>
              <div class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm font-semibold text-foreground">
                {{ shiftType === 'NIGHT' ? 'Nocturno (máx. 7 h)' : 'Continuo (máx. 8 h)' }}
              </div>
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

          <!-- Horarios: Inicio y Fin -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
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

          </div>

          <p class="rounded-xl border border-blue-200 bg-blue-50 p-3 text-xs font-medium text-blue-800">
            Los descansos se registran después de crear el turno y los días laborales se definen al asignarlo a cada empleado.
          </p>

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
                Habilita si la jornada laboral finaliza al día siguiente (Turno Nocturno).
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
                <span *ngIf="effectiveCalculation.isOvernight || crossesMidnight" class="inline-flex items-center gap-1 rounded-full bg-indigo-500/20 px-2 py-0.5 text-[10px] font-bold text-indigo-700">
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
  startTime = '08:30';
  endTime = '16:30';
  lateToleranceMinutes = 10;
  earlyDepartureToleranceMinutes = 5;
  crossesMidnight = false;

  get shiftType(): ShiftType {
    const startHour = Number(this.startTime.substring(0, 2));
    const endHour = Number(this.endTime.substring(0, 2));
    return this.crossesMidnight || startHour >= 18 || endHour <= 6 ? 'NIGHT' : 'CONTINUOUS';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['shiftToEdit'] && this.shiftToEdit) {
      this.code = this.shiftToEdit.code;
      this.name = this.shiftToEdit.name;
      this.startTime = this.shiftToEdit.startTime.substring(0, 5);
      this.endTime = this.shiftToEdit.endTime.substring(0, 5);
      this.lateToleranceMinutes = this.shiftToEdit.lateToleranceMinutes ?? this.shiftToEdit.gracePeriodMinutes ?? 10;
      this.earlyDepartureToleranceMinutes = this.shiftToEdit.earlyDepartureToleranceMinutes ?? 5;
      this.crossesMidnight = this.shiftToEdit.crossesMidnight ?? this.shiftToEdit.isOvernight ?? false;
    } else if (changes['isOpen'] && this.isOpen && !this.shiftToEdit) {
      this.resetForm();
    }
  }

  get effectiveCalculation() {
    const calc = this.formUseCase.calculateEffectiveHours(this.startTime, this.endTime, 0);
    return calc;
  }

  get lawValidation() {
    return this.formUseCase.validateLaborLaw({
      code: this.code,
      name: this.name,
      shiftType: this.shiftType,
      startTime: this.startTime,
      endTime: this.endTime,
      breakMinutes: 0,
      lateToleranceMinutes: this.lateToleranceMinutes,
      earlyDepartureToleranceMinutes: this.earlyDepartureToleranceMinutes,
      gracePeriodMinutes: this.lateToleranceMinutes,
      crossesMidnight: this.crossesMidnight,
      isOvernight: this.crossesMidnight || this.effectiveCalculation.isOvernight
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
        breakMinutes: 0,
        lateToleranceMinutes: this.lateToleranceMinutes,
        earlyDepartureToleranceMinutes: this.earlyDepartureToleranceMinutes,
        gracePeriodMinutes: this.lateToleranceMinutes,
        crossesMidnight: this.crossesMidnight,
        isOvernight: this.crossesMidnight || this.effectiveCalculation.isOvernight
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
    this.startTime = '08:30';
    this.endTime = '16:30';
    this.lateToleranceMinutes = 10;
    this.earlyDepartureToleranceMinutes = 5;
    this.crossesMidnight = false;
  }
}
