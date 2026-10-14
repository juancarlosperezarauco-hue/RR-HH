import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { LeaveTypeResponse, LeaveBalanceResponse, HolidayResponse } from '../../../../entities/absence';
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
        class="w-full max-w-xl rounded-2xl border border-border bg-card p-6 shadow-2xl transition-all max-h-[90vh] overflow-y-auto"
        (click)="$event.stopPropagation()"
      >
        <!-- Modal Header -->
        <div class="flex items-start justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-lg font-bold text-foreground">Solicitud de Ausencia o Permiso</h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Cómputo en días hábiles conforme a la Ley General del Trabajo de Bolivia
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
          <!-- Tipo de Licencia -->
          <div>
            <label class="block text-xs font-semibold text-foreground mb-1">
              Tipo de Permiso / Ausencia <span class="text-rose-500">*</span>
            </label>
            <select
              [(ngModel)]="selectedTypeId"
              (ngModelChange)="onTypeChange()"
              name="selectedTypeId"
              class="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm font-medium text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
              required
            >
              <option *ngFor="let t of leaveTypes" [value]="t.id">
                {{ t.name }} {{ t.isPaid ? '(Remunerada)' : '(No Remunerada)' }}
              </option>
            </select>
            <p *ngIf="selectedType?.description" class="mt-1 text-xs text-muted-foreground">
              {{ selectedType?.description }}
            </p>
          </div>

          <!-- Rango de Fechas -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">
                Fecha Inicio <span class="text-rose-500">*</span>
              </label>
              <input
                type="date"
                [(ngModel)]="startDate"
                (ngModelChange)="recalculateDays()"
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
                name="endDate"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                required
              />
            </div>
          </div>

          <!-- Cómputo de Días Hábiles & Alertas -->
          <div class="rounded-xl border border-border bg-accent/40 p-3.5 space-y-2">
            <div class="flex items-center justify-between text-xs">
              <span class="text-muted-foreground">Duración en Días Hábiles:</span>
              <span class="font-mono text-sm font-bold text-foreground">
                {{ computedDays }} {{ computedDays === 1 ? 'día hábil' : 'días hábiles' }}
              </span>
            </div>
            <p class="text-[11px] text-muted-foreground">
              * Descuenta sábados, domingos y feriados nacionales oficiales (LGT).
            </p>

            <!-- Alerta: Saldo insuficiente de vacación -->
            <div
              *ngIf="isVacation && balance && computedDays > balance.remainingDays"
              class="rounded-lg bg-rose-500/10 border border-rose-500/30 p-2.5 flex items-start gap-2 text-xs text-rose-700 dark:text-rose-400"
            >
              <lucide-icon name="alert-triangle" class="h-4 w-4 shrink-0 mt-0.5"></lucide-icon>
              <span>
                Los días solicitados (<strong>{{ computedDays }}</strong>) superan su saldo disponible (<strong>{{ balance.remainingDays }} días</strong>).
              </span>
            </div>
          </div>

          <!-- Documento Probatorio / Respaldo (Vínculo HU15) -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs font-semibold text-foreground">
                Documento Probatorio / Respaldo
                <span *ngIf="selectedType?.requiresAttachment" class="text-rose-500">* (Obligatorio)</span>
              </label>
              <span *ngIf="selectedType?.requiresAttachment" class="text-[11px] text-primary font-medium">
                Se archivará en Expediente (HU15)
              </span>
            </div>
            <input
              type="file"
              (change)="onFileSelected($event)"
              accept=".pdf,.png,.jpg,.jpeg,.docx"
              class="w-full rounded-xl border border-border bg-background px-3 py-2 text-xs text-foreground file:mr-3 file:py-1 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-primary file:text-primary-foreground hover:file:bg-primary/90"
            />
            <p *ngIf="selectedType?.requiresAttachment && !proofFile" class="mt-1 text-xs text-rose-500 font-medium">
              Este tipo de ausencia requiere adjuntar un documento probatorio obligatorio.
            </p>
          </div>

          <!-- Motivo -->
          <div>
            <label class="block text-xs font-semibold text-foreground mb-1">
              Motivo o Justificación <span class="text-rose-500">*</span>
            </label>
            <textarea
              [(ngModel)]="reason"
              name="reason"
              rows="3"
              placeholder="Explique el motivo de la ausencia solicitada..."
              class="w-full rounded-xl border border-border bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary"
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
              Enviar Solicitud
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class AbsenceRequestModalComponent {
  readonly useCase = inject(AbsenceRequestUseCase);

  @Input() isOpen = false;
  @Input() employeeId = '';
  @Input() leaveTypes: LeaveTypeResponse[] = [];
  @Input() balance: LeaveBalanceResponse | null = null;
  @Input() holidays: HolidayResponse[] = [];

  @Output() closeModal = new EventEmitter<void>();
  @Output() requestSubmitted = new EventEmitter<void>();

  selectedTypeId = '';
  startDate = '';
  endDate = '';
  reason = '';
  proofFile: File | null = null;
  computedDays = 0;

  get selectedType(): LeaveTypeResponse | undefined {
    return this.leaveTypes.find(t => t.id === this.selectedTypeId);
  }

  get isVacation(): boolean {
    return this.selectedType?.code === 'LEAVE_VACATION';
  }

  get isSubmitDisabled(): boolean {
    if (this.useCase.submitting()) return true;
    if (!this.employeeId) return true;
    if (!this.selectedTypeId || !this.startDate || !this.endDate || !this.reason.trim()) return true;
    if (this.computedDays <= 0) return true;
    if (this.isVacation && this.balance && this.computedDays > this.balance.remainingDays) return true;
    if (this.selectedType?.requiresAttachment && !this.proofFile) return true;
    return false;
  }

  onTypeChange(): void {
    this.recalculateDays();
  }

  recalculateDays(): void {
    if (!this.startDate || !this.endDate) {
      this.computedDays = 0;
      return;
    }
    const holidayDates = this.holidays.map(h => h.holidayDate);
    this.computedDays = this.useCase.calculateBusinessDays(this.startDate, this.endDate, holidayDates);
  }

  onFileSelected(event: any): void {
    const file = event.target.files?.[0];
    this.proofFile = file || null;
  }

  close(): void {
    this.startDate = '';
    this.endDate = '';
    this.reason = '';
    this.proofFile = null;
    this.computedDays = 0;
    this.closeModal.emit();
  }

  async onSubmit(): Promise<void> {
    if (this.isSubmitDisabled) return;

    const holidayDates = this.holidays.map(h => h.holidayDate);
    const ok = await this.useCase.submitRequest(
      {
        employeeId: this.employeeId,
        leaveTypeId: this.selectedTypeId,
        startDate: this.startDate,
        endDate: this.endDate,
        reason: this.reason.trim(),
        proofDocument: this.proofFile
      },
      this.isVacation,
      this.balance?.remainingDays,
      this.selectedType?.requiresAttachment,
      holidayDates
    );

    if (ok) {
      this.close();
      this.requestSubmitted.emit();
    }
  }
}
