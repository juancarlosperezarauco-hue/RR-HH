import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { ShiftResponse, WorkShiftRequest } from '../../../../entities/shift';
import { ShiftFormUseCase } from '../../application/shift-form.usecase';

@Component({
  selector: 'app-shift-form-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div
      *ngIf="isOpen"
      class="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div
        class="app-modal-panel relative flex max-h-[92vh] w-full max-w-xl flex-col overflow-hidden rounded-[24px] border border-border bg-card shadow-2xl">
        
        <!-- Header fijo -->
        <div class="flex shrink-0 items-center justify-between border-b border-border px-6 py-4">
          <div>
            <h3 class="text-xl font-bold text-accent-foreground">
              {{ shiftToEdit ? 'Editar Turno Laboral' : 'Nuevo Turno de Trabajo' }}
            </h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Define horarios, cruces de medianoche y tolerancias de marcación.
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
        <form (ngSubmit)="submit()" novalidate class="flex min-h-0 flex-1 flex-col overflow-hidden">
          <div class="flex-1 space-y-4 overflow-y-auto p-6">
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
              [class.border-destructive]="submitted && !name.trim()"
              class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm text-foreground outline-none transition-all focus:border-primary focus:ring-1 focus:ring-primary" />
            <p *ngIf="submitted && !name.trim()" class="mt-1 text-xs font-medium text-destructive">
              El nombre descriptivo del turno es obligatorio.
            </p>
          </div>

          <div>
            <label class="block text-xs font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
              Descripción (opcional)
            </label>
            <textarea
              [(ngModel)]="description"
              name="description"
              rows="2"
              placeholder="Ej: Jornada administrativa de lunes a viernes"
              class="w-full resize-none rounded-xl border border-border bg-muted/40 px-3.5 py-2.5 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary"></textarea>
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
                [class.border-destructive]="submitted && !startTime"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
              <p *ngIf="submitted && !startTime" class="mt-1 text-xs font-medium text-destructive">Indica la hora de inicio.</p>
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
                [class.border-destructive]="submitted && !endTime"
                class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
              <p *ngIf="submitted && !endTime" class="mt-1 text-xs font-medium text-destructive">Indica la hora de finalización.</p>
            </div>

          </div>

          <!-- Tolerancias -->
          <div class="space-y-2">
            <p class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Tolerancia</p>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <div class="flex items-center justify-between mb-1.5">
                <label class="block text-xs font-medium text-muted-foreground">
                  Entrada
                </label>
                <span class="text-xs font-bold text-primary">{{ lateToleranceMinutes }} min</span>
              </div>
              <input
                type="number"
                [(ngModel)]="lateToleranceMinutes"
                name="lateToleranceMinutes"
                min="0"
                max="1440"
                step="1"
                class="w-full rounded-xl border border-border bg-muted/40 px-3 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>

            <div>
              <div class="flex items-center justify-between mb-1.5">
                <label class="block text-xs font-medium text-muted-foreground">
                  Salida
                </label>
                <span class="text-xs font-bold text-primary">{{ earlyDepartureToleranceMinutes }} min</span>
              </div>
              <input
                type="number"
                [(ngModel)]="earlyDepartureToleranceMinutes"
                name="earlyDepartureToleranceMinutes"
                min="0"
                max="1440"
                step="1"
                class="w-full rounded-xl border border-border bg-muted/40 px-3 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
            </div>
            </div>
          </div>

          <!-- Cruce de Medianoche (Toggle explícito) -->
          <div class="flex items-center justify-between rounded-xl border border-border bg-muted/20 p-3">
            <div class="space-y-0.5">
              <p class="text-xs font-bold text-accent-foreground">Cruza Medianoche (+1 Día)</p>
              <p class="text-[11px] text-muted-foreground">
                Actívalo cuando la hora de fin pertenece al día siguiente.
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
                Resumen de la jornada
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
              <span>Configuración válida para las reglas de horario y tolerancias.</span>
            </div>
          </div>

          </div>

          <!-- Botones de Acción (Footer fijo) -->
          <div class="flex shrink-0 items-center justify-end gap-3 border-t border-border bg-card px-6 py-4">
            <button
              type="button"
              (click)="close()"
              class="rounded-full border border-border bg-card px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-accent hover:text-foreground cursor-pointer">
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="formUseCase.saving()"
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

  name = '';
  description = '';
  startTime = '08:30';
  endTime = '16:30';
  lateToleranceMinutes = 10;
  earlyDepartureToleranceMinutes = 5;
  crossesMidnight = false;
  submitted = false;

  ngOnChanges(_changes: SimpleChanges): void {
    if (this.isOpen && this.shiftToEdit) {
      this.submitted = false;
      this.name = this.shiftToEdit.name;
      this.description = this.shiftToEdit.description ?? '';
      this.startTime = this.shiftToEdit.startTime ? this.shiftToEdit.startTime.substring(0, 5) : '08:30';
      this.endTime = this.shiftToEdit.endTime ? this.shiftToEdit.endTime.substring(0, 5) : '16:30';
      this.lateToleranceMinutes = this.shiftToEdit.lateToleranceMinutes ?? this.shiftToEdit.gracePeriodMinutes ?? 10;
      this.earlyDepartureToleranceMinutes = this.shiftToEdit.earlyDepartureToleranceMinutes ?? 5;
      this.crossesMidnight = this.shiftToEdit.crossesMidnight ?? this.shiftToEdit.isOvernight ?? false;
    } else if (this.isOpen && !this.shiftToEdit) {
      this.resetForm();
    }
  }

  get effectiveCalculation() {
    const calc = this.formUseCase.calculateEffectiveHours(this.startTime, this.endTime, 0);
    return calc;
  }

  get lawValidation() {
    const request: WorkShiftRequest = {
      name: this.name,
      description: this.description.trim() || undefined,
      startTime: this.startTime,
      endTime: this.endTime,
      crossesMidnight: this.crossesMidnight,
      lateToleranceMinutes: this.lateToleranceMinutes,
      earlyDepartureToleranceMinutes: this.earlyDepartureToleranceMinutes
    };
    return this.formUseCase.validateLaborLaw(request);
  }

  isValid(): boolean {
    return !!this.name.trim() && !!this.startTime && !!this.endTime && this.lawValidation.isValid;
  }

  async submit(): Promise<void> {
    this.submitted = true;
    if (!this.isValid()) return;

    const request: WorkShiftRequest = {
      name: this.name.trim(),
      description: this.description.trim() || undefined,
      startTime: this.startTime.length === 5 ? `${this.startTime}:00` : this.startTime,
      endTime: this.endTime.length === 5 ? `${this.endTime}:00` : this.endTime,
      crossesMidnight: this.crossesMidnight,
      lateToleranceMinutes: this.lateToleranceMinutes,
      earlyDepartureToleranceMinutes: this.earlyDepartureToleranceMinutes
    };
    const ok = await this.formUseCase.saveShift(request, this.shiftToEdit?.id);

    if (ok) {
      this.saved.emit();
      this.close();
    }
  }

  close(): void {
    this.submitted = false;
    this.closeModal.emit();
  }

  private resetForm(): void {
    this.submitted = false;
    this.name = '';
    this.description = '';
    this.startTime = '08:30';
    this.endTime = '16:30';
    this.lateToleranceMinutes = 10;
    this.earlyDepartureToleranceMinutes = 5;
    this.crossesMidnight = false;
  }
}
