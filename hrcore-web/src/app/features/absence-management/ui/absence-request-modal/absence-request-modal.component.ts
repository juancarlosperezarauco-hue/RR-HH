import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { LeaveTypeResponse, LeaveBalanceResponse, HolidayResponse } from '../../../../entities/absence';
import { EmployeeSummary } from '../../../../entities/employees';
import { AbsenceRequestUseCase } from '../../application/absence-request.usecase';

@Component({
  selector: 'app-absence-request-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div
      *ngIf="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200"
    >
      <div
        class="w-full max-w-xl rounded-2xl border border-border bg-card p-6 shadow-2xl transition-all max-h-[92vh] overflow-y-auto"
        (click)="$event.stopPropagation()"
      >
        <!-- Modal Header -->
        <div class="flex items-start justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-lg font-bold text-foreground">Registrar Ausencia o Permiso</h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Control laboral de ausencias con cómputo de días hábiles conforme a la Ley General del Trabajo
            </p>
          </div>
          <button
            type="button"
            (click)="close()"
            class="rounded-lg p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
          </button>
        </div>

        <form (ngSubmit)="onSubmit()" class="mt-4 space-y-4">
          <!-- 1. Selección de Trabajador / Colaborador -->
          <div>
            <label class="block text-xs font-semibold text-foreground mb-1">
              Colaborador / Trabajador <span class="text-rose-500">*</span>
            </label>
            <select
              [(ngModel)]="selectedEmployeeId"
              (ngModelChange)="onEmployeeChange()"
              name="selectedEmployeeId"
              class="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm font-medium text-foreground focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
              required
            >
              <option value="" disabled>Seleccione un colaborador...</option>
              <option *ngFor="let emp of employees" [value]="emp.id">
                {{ emp.employeeNumber }} · {{ emp.firstName }} {{ emp.lastName }}
              </option>
            </select>
            <div *ngIf="selectedEmployee" class="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
              <span class="inline-flex items-center gap-1 font-medium text-foreground">
                <lucide-icon name="user" class="h-3.5 w-3.5 text-primary"></lucide-icon>
                {{ selectedEmployee.firstName }} {{ selectedEmployee.lastName }}
              </span>
              <span>•</span>
              <span>Código: <strong class="text-foreground">{{ selectedEmployee.employeeNumber }}</strong></span>
              <span>•</span>
              <span class="inline-flex items-center rounded-md bg-emerald-500/10 px-2 py-0.5 text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                {{ selectedEmployee.employmentStatus }}
              </span>
            </div>
          </div>

          <!-- 2. Tipo de Permiso o Ausencia -->
          <div>
            <label class="block text-xs font-semibold text-foreground mb-1">
              Tipo de Permiso / Ausencia <span class="text-rose-500">*</span>
            </label>
            <select
              [(ngModel)]="selectedTypeId"
              (ngModelChange)="onTypeChange()"
              name="selectedTypeId"
              class="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm font-medium text-foreground focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
              required
            >
              <option value="" disabled>Seleccione el tipo de ausencia...</option>
              <option *ngFor="let t of leaveTypes" [value]="t.id">
                {{ t.name }} {{ t.paid ? '(Remunerada)' : '(No Remunerada)' }}
              </option>
            </select>

            <!-- Metadata del Tipo de Ausencia -->
            <div *ngIf="selectedType" class="mt-2 space-y-1.5">
              <p *ngIf="selectedType.description" class="text-xs text-muted-foreground">
                {{ selectedType.description }}
              </p>
              <div class="flex flex-wrap items-center gap-1.5 pt-1">
                <span
                  class="inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-medium"
                  [ngClass]="selectedType.paid ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20' : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20'"
                >
                  <lucide-icon [name]="selectedType.paid ? 'dollar-sign' : 'clock'" class="h-3 w-3"></lucide-icon>
                  {{ selectedType.paid ? 'Remunerada' : 'Sin goce de haberes' }}
                </span>

                <span
                  *ngIf="selectedType.requiresApproval"
                  class="inline-flex items-center gap-1 rounded-md bg-blue-500/10 text-blue-600 dark:text-blue-400 px-2 py-0.5 text-[11px] font-medium border border-blue-500/20"
                >
                  <lucide-icon name="user-check" class="h-3 w-3"></lucide-icon>
                  Requiere Aprobación
                </span>

                <span
                  *ngIf="!selectedType.requiresApproval"
                  class="inline-flex items-center gap-1 rounded-md bg-purple-500/10 text-purple-600 dark:text-purple-400 px-2 py-0.5 text-[11px] font-medium border border-purple-500/20"
                >
                  <lucide-icon name="zap" class="h-3 w-3"></lucide-icon>
                  Aprobación Automática
                </span>

                <span
                  *ngIf="selectedType.requiresAttachment"
                  class="inline-flex items-center gap-1 rounded-md bg-rose-500/10 text-rose-600 dark:text-rose-400 px-2 py-0.5 text-[11px] font-medium border border-rose-500/20"
                >
                  <lucide-icon name="file-text" class="h-3 w-3"></lucide-icon>
                  Respaldo Obligatorio
                </span>

                <span
                  *ngIf="selectedType.deductsBalance"
                  class="inline-flex items-center gap-1 rounded-md bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 px-2 py-0.5 text-[11px] font-medium border border-indigo-500/20"
                >
                  <lucide-icon name="calendar" class="h-3 w-3"></lucide-icon>
                  Descuenta Saldo Vacación
                </span>
              </div>
            </div>
          </div>

          <!-- 3. Opción de Permiso por Horas (Fracción de jornada) -->
          <div class="rounded-xl border border-border/80 bg-accent/20 p-3">
            <label class="flex items-center gap-2 cursor-pointer text-xs font-medium text-foreground">
              <input
                type="checkbox"
                [(ngModel)]="isPartialDay"
                (ngModelChange)="onPartialDayToggle()"
                name="isPartialDay"
                class="rounded border-border text-primary focus:ring-primary h-4 w-4"
              />
              <span>Permiso por horas (fracción de jornada en un único día)</span>
            </label>
          </div>

          <!-- 4. Rango de Fechas / Horas -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">
                Fecha Inicio <span class="text-rose-500">*</span>
              </label>
              <input
                type="date"
                [(ngModel)]="startDate"
                (ngModelChange)="onStartDateChange()"
                name="startDate"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                required
              />
            </div>
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">
                Fecha Fin <span class="text-rose-500">*</span>
              </label>
              <input
                type="date"
                [(ngModel)]="endDate"
                (ngModelChange)="recalculateDays()"
                [disabled]="isPartialDay"
                name="endDate"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary disabled:opacity-60"
                required
              />
            </div>
          </div>

          <!-- Horas si es parcial -->
          <div *ngIf="isPartialDay" class="grid grid-cols-1 sm:grid-cols-2 gap-4 animate-in fade-in duration-150">
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">
                Hora Inicio <span class="text-rose-500">*</span>
              </label>
              <input
                type="time"
                [(ngModel)]="startTime"
                (ngModelChange)="recalculateDays()"
                name="startTime"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                required
              />
            </div>
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">
                Hora Fin <span class="text-rose-500">*</span>
              </label>
              <input
                type="time"
                [(ngModel)]="endTime"
                (ngModelChange)="recalculateDays()"
                name="endTime"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                required
              />
            </div>
          </div>

          <!-- 5. Cómputo de Días Hábiles & Resumen -->
          <div class="rounded-xl border border-border bg-accent/30 p-3.5 space-y-2">
            <div class="flex items-center justify-between text-xs">
              <span class="text-muted-foreground">Duración Estimada:</span>
              <span *ngIf="!isPartialDay" class="font-mono text-sm font-bold text-foreground">
                {{ computedDays }} {{ computedDays === 1 ? 'día hábil' : 'días hábiles' }}
              </span>
              <span *ngIf="isPartialDay" class="font-mono text-sm font-bold text-foreground">
                {{ computedHours }} horas
              </span>
            </div>
            <p class="text-[11px] text-muted-foreground">
              * Descuenta fines de semana y feriados oficiales. El cómputo final es validado con el turno de trabajo asignado del colaborador.
            </p>

            <!-- Saldo de Vacaciones disponible -->
            <div
              *ngIf="isVacation && balance"
              class="flex items-center justify-between rounded-lg bg-background/60 border border-border/40 px-2.5 py-1.5 text-xs"
            >
              <span class="text-muted-foreground">Saldo disponible de vacación:</span>
              <span class="font-mono font-bold" [ngClass]="balance.remainingDays > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-amber-600 dark:text-amber-400'">
                {{ balance.remainingDays }} días
              </span>
            </div>

            <!-- Alerta informativa si supera saldo -->
            <div
              *ngIf="isVacation && balance && balance.remainingDays > 0 && computedDays > balance.remainingDays"
              class="rounded-lg bg-rose-500/10 border border-rose-500/30 p-2.5 flex items-start gap-2 text-xs text-rose-700 dark:text-rose-400"
            >
              <lucide-icon name="alert-triangle" class="h-4 w-4 shrink-0 mt-0.5"></lucide-icon>
              <span>
                Los días solicitados (<strong>{{ computedDays }}</strong>) superan el saldo disponible registrado (<strong>{{ balance.remainingDays }} días</strong>).
              </span>
            </div>
          </div>

          <!-- 6. Documento de Respaldo / Justificativo -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs font-semibold text-foreground">
                Documento Probatorio / Respaldo
                <span *ngIf="selectedType?.requiresAttachment" class="text-rose-500">* (Obligatorio)</span>
                <span *ngIf="!selectedType?.requiresAttachment" class="text-muted-foreground font-normal">(Opcional)</span>
              </label>
              <span class="text-[11px] text-muted-foreground font-mono">
                PDF, PNG, JPG (máx. 10MB)
              </span>
            </div>

            <div class="flex items-center gap-2">
              <input
                type="file"
                (change)="onFileSelected($event)"
                accept=".pdf,.png,.jpg,.jpeg"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground file:mr-3 file:py-1 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 transition-colors"
              />
              <button
                *ngIf="proofFile"
                type="button"
                (click)="clearFile()"
                class="rounded-lg border border-border p-2 text-muted-foreground hover:text-foreground hover:bg-muted"
                title="Quitar archivo adjunto"
              >
                <lucide-icon name="trash-2" class="h-4 w-4"></lucide-icon>
              </button>
            </div>

            <div *ngIf="proofFile" class="mt-1 flex items-center gap-1.5 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
              <lucide-icon name="check" class="h-3.5 w-3.5"></lucide-icon>
              <span>Adjunto listo: {{ proofFile.name }} ({{ formatFileSize(proofFile.size) }})</span>
            </div>

            <p *ngIf="selectedType?.requiresAttachment && !proofFile" class="mt-1 text-xs text-rose-500 font-medium">
              Este tipo de permiso requiere obligatoriamente adjuntar un documento probatorio de respaldo.
            </p>
          </div>

          <!-- 7. Motivo / Justificación -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs font-semibold text-foreground">
                Motivo o Justificación <span class="text-rose-500">*</span>
              </label>
              <span class="text-[11px] text-muted-foreground font-mono">
                {{ reason.trim().length }}/5 mín.
              </span>
            </div>
            <textarea
              [(ngModel)]="reason"
              name="reason"
              rows="3"
              placeholder="Explique detalladamente el motivo del permiso o ausencia solicitada..."
              class="w-full rounded-xl border border-border bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
              required
            ></textarea>
          </div>

          <!-- Modal Actions -->
          <div class="flex items-center justify-end gap-3 pt-3 border-t border-border">
            <button
              type="button"
              (click)="close()"
              [disabled]="useCase.submitting()"
              class="rounded-xl border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="isSubmitDisabled"
              class="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-xs"
            >
              <lucide-icon *ngIf="useCase.submitting()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
              <lucide-icon *ngIf="!useCase.submitting()" name="send" class="h-4 w-4"></lucide-icon>
              <span>Registrar y Enviar Solicitud</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class AbsenceRequestModalComponent implements OnChanges {
  readonly useCase = inject(AbsenceRequestUseCase);

  @Input() isOpen = false;
  @Input() employeeId = '';
  @Input() employees: EmployeeSummary[] = [];
  @Input() leaveTypes: LeaveTypeResponse[] = [];
  @Input() balance: LeaveBalanceResponse | null = null;
  @Input() holidays: HolidayResponse[] = [];

  @Output() closeModal = new EventEmitter<void>();
  @Output() requestSubmitted = new EventEmitter<void>();
  @Output() employeeSelected = new EventEmitter<string>();

  selectedEmployeeId = '';
  selectedTypeId = '';
  startDate = '';
  endDate = '';
  isPartialDay = false;
  startTime = '';
  endTime = '';
  reason = '';
  proofFile: File | null = null;
  computedDays = 0;
  computedHours = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      if (this.employeeId) {
        this.selectedEmployeeId = this.employeeId;
      } else if (this.employees.length > 0 && !this.selectedEmployeeId) {
        this.selectedEmployeeId = this.employees[0].id;
      }

      if (this.leaveTypes.length > 0 && !this.selectedTypeId) {
        this.selectedTypeId = this.leaveTypes[0].id;
      }

      if (!this.startDate) {
        const todayIso = new Date().toISOString().substring(0, 10);
        this.startDate = todayIso;
        this.endDate = todayIso;
      }

      this.recalculateDays();
      if (this.selectedEmployeeId) {
        this.employeeSelected.emit(this.selectedEmployeeId);
      }
    }

    if (changes['employeeId'] && this.employeeId && !this.selectedEmployeeId) {
      this.selectedEmployeeId = this.employeeId;
    }
  }

  get selectedEmployee(): EmployeeSummary | undefined {
    return this.employees.find(e => e.id === this.selectedEmployeeId);
  }

  get selectedType(): LeaveTypeResponse | undefined {
    return this.leaveTypes.find(t => t.id === this.selectedTypeId);
  }

  get isVacation(): boolean {
    return this.selectedType?.code === 'LEAVE_VACATION' || !!this.selectedType?.deductsBalance;
  }

  get isSubmitDisabled(): boolean {
    if (this.useCase.submitting()) return true;
    if (!this.selectedEmployeeId || !this.selectedTypeId || !this.startDate || !this.endDate || this.reason.trim().length < 5) {
      return true;
    }
    if (this.isPartialDay) {
      if (!this.startTime || !this.endTime || this.endTime <= this.startTime) return true;
    } else {
      if (this.computedDays <= 0) return true;
    }
    if (this.selectedType?.requiresAttachment && !this.proofFile) return true;
    return false;
  }

  onEmployeeChange(): void {
    this.employeeSelected.emit(this.selectedEmployeeId);
    this.recalculateDays();
  }

  onTypeChange(): void {
    this.recalculateDays();
  }

  onPartialDayToggle(): void {
    if (this.isPartialDay) {
      this.endDate = this.startDate;
      if (!this.startTime) this.startTime = '09:00';
      if (!this.endTime) this.endTime = '13:00';
    } else {
      this.startTime = '';
      this.endTime = '';
    }
    this.recalculateDays();
  }

  onStartDateChange(): void {
    if (this.isPartialDay || !this.endDate || this.endDate < this.startDate) {
      this.endDate = this.startDate;
    }
    this.recalculateDays();
  }

  recalculateDays(): void {
    if (!this.startDate || !this.endDate) {
      this.computedDays = 0;
      this.computedHours = 0;
      return;
    }

    if (this.isPartialDay && this.startTime && this.endTime) {
      const [sh, sm] = this.startTime.split(':').map(Number);
      const [eh, em] = this.endTime.split(':').map(Number);
      const diffMins = (eh * 60 + em) - (sh * 60 + sm);
      this.computedHours = diffMins > 0 ? Math.round((diffMins / 60) * 10) / 10 : 0;
    }

    const holidayDates = this.holidays.map(h => h.holidayDate);
    this.computedDays = this.useCase.calculateBusinessDays(this.startDate, this.endDate, holidayDates);
  }

  onFileSelected(event: any): void {
    const file = event.target.files?.[0];
    this.proofFile = file || null;
  }

  clearFile(): void {
    this.proofFile = null;
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  close(): void {
    this.isPartialDay = false;
    this.startTime = '';
    this.endTime = '';
    this.reason = '';
    this.proofFile = null;
    this.computedDays = 0;
    this.computedHours = 0;
    this.closeModal.emit();
  }

  async onSubmit(): Promise<void> {
    if (this.isSubmitDisabled) return;

    const holidayDates = this.holidays.map(h => h.holidayDate);
    const ok = await this.useCase.submitRequest(
      {
        employeeId: this.selectedEmployeeId,
        leaveTypeId: this.selectedTypeId,
        startDate: this.startDate,
        endDate: this.endDate,
        startTime: this.isPartialDay ? this.startTime : null,
        endTime: this.isPartialDay ? this.endTime : null,
        reason: this.reason.trim(),
        proofDocument: this.proofFile
      },
      this.isVacation,
      this.balance?.remainingDays,
      this.selectedType?.requiresAttachment,
      holidayDates,
      this.selectedType?.deductsBalance
    );

    if (ok) {
      this.close();
      this.requestSubmitted.emit();
    }
  }
}
