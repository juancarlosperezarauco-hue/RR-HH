import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { AttendanceRecordResponse, AdjustmentType } from '../../../../entities/attendance';
import { AttendanceRegularizeUseCase } from '../../application/attendance-regularize.usecase';

@Component({
  selector: 'app-attendance-regularize-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div
      *ngIf="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200"
    >
      <div
        class="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-2xl transition-all"
        (click)="$event.stopPropagation()"
      >
        <!-- Modal Header -->
        <div class="flex items-start justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-lg font-bold text-foreground">Regularización de Asistencia</h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Ajuste manual auditado de marcación para validez laboral y legal
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

        <!-- Colaborador y Registro Original -->
        <div *ngIf="record" class="mt-4 rounded-xl bg-accent/40 border border-border/60 p-3.5 space-y-1.5">
          <div class="flex items-center justify-between text-xs">
            <span class="font-bold text-foreground">{{ record.employeeFullName || 'Colaborador' }}</span>
            <span class="font-mono text-muted-foreground">{{ record.employeeCode }}</span>
          </div>
          <div class="flex items-center justify-between text-xs text-muted-foreground">
            <span>Fecha: <strong>{{ record.workDate }}</strong></span>
            <span>Entrada Actual: <strong>{{ formatTime(record.checkInTime) }}</strong></span>
            <span>Salida Actual: <strong>{{ formatTime(record.checkOutTime) }}</strong></span>
          </div>
        </div>

        <!-- Formulario -->
        <form (ngSubmit)="onSubmit()" class="mt-4 space-y-4">
          <!-- Tipo de Regularización -->
          <div>
            <label class="block text-xs font-semibold text-foreground mb-1">
              Tipo de Ajuste / Regularización <span class="text-rose-500">*</span>
            </label>
            <select
              [(ngModel)]="adjustmentType"
              name="adjustmentType"
              class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
            >
              <option value="TIME_CORRECTION">Corrección Horaria (Entrada / Salida)</option>
              <option value="MISSING_PUNCH">Marcación Omitida / Olvidada</option>
              <option value="STATUS_CORRECTION">Corrección de Estado de Jornada</option>
              <option value="OTHER">Otro Motivo Justificado</option>
            </select>
          </div>

          <!-- Corrección Horaria -->
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">
                Hora de Entrada Corregida
              </label>
              <input
                type="time"
                [(ngModel)]="correctedCheckInTime"
                name="correctedCheckInTime"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
              />
            </div>
            <div>
              <label class="block text-xs font-semibold text-foreground mb-1">
                Hora de Salida Corregida
              </label>
              <input
                type="time"
                [(ngModel)]="correctedCheckOutTime"
                name="correctedCheckOutTime"
                class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
              />
            </div>
          </div>

          <!-- Justificación Obligatoria (Min 10 caracteres) -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs font-semibold text-foreground">
                Motivo / Justificación <span class="text-rose-500">*</span>
              </label>
              <span
                class="text-[11px] font-mono font-medium"
                [ngClass]="reason.trim().length >= 10 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'"
              >
                {{ reason.trim().length }}/10 mín.
              </span>
            </div>
            <textarea
              [(ngModel)]="reason"
              name="reason"
              rows="3"
              placeholder="Describa el motivo del ajuste (ej. Falla en lector biométrico, comisión externa verificada...)"
              class="w-full rounded-xl border border-border bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary"
              required
            ></textarea>

            <p *ngIf="reasonTouched && reason.trim().length < 10" class="mt-1 text-xs text-rose-500 font-medium flex items-center gap-1">
              <lucide-icon name="alert-circle" class="h-3.5 w-3.5"></lucide-icon>
              La justificación es obligatoria y debe tener al menos 10 caracteres.
            </p>
          </div>

          <!-- Advertencia de Auditoría Legal Inmutable -->
          <div class="rounded-xl border border-primary/20 bg-primary/5 p-3 flex items-start gap-2.5 text-xs text-primary">
            <lucide-icon name="shield-check" class="h-4 w-4 shrink-0 mt-0.5"></lucide-icon>
            <p class="leading-relaxed">
              <strong>Aviso de Auditoría:</strong> Esta regularización quedará registrada de forma inmutable con su usuario y timestamp para efectos de fiscalización laboral (LGT Bolivia).
            </p>
          </div>

          <!-- Modal Actions -->
          <div class="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              (click)="close()"
              [disabled]="useCase.submitting()"
              class="rounded-xl border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent disabled:opacity-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="useCase.submitting() || reason.trim().length < 10"
              class="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-xs"
            >
              <lucide-icon *ngIf="useCase.submitting()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
              <lucide-icon *ngIf="!useCase.submitting()" name="check-circle" class="h-4 w-4"></lucide-icon>
              Guardar Regularización
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class AttendanceRegularizeDialogComponent {
  readonly useCase = inject(AttendanceRegularizeUseCase);

  @Input() isOpen = false;
  @Input() record: AttendanceRecordResponse | null = null;
  @Output() closeDialog = new EventEmitter<void>();
  @Output() regularized = new EventEmitter<void>();

  adjustmentType: AdjustmentType = 'TIME_CORRECTION';
  correctedCheckInTime = '08:30';
  correctedCheckOutTime = '16:30';
  reason = '';
  reasonTouched = false;

  close(): void {
    this.reason = '';
    this.reasonTouched = false;
    this.closeDialog.emit();
  }

  async onSubmit(): Promise<void> {
    this.reasonTouched = true;
    if (!this.record || this.reason.trim().length < 10) {
      return;
    }

    const todayDate = this.record.workDate;
    const fullCheckIn = this.correctedCheckInTime ? `${todayDate}T${this.correctedCheckInTime}:00Z` : undefined;
    const fullCheckOut = this.correctedCheckOutTime ? `${todayDate}T${this.correctedCheckOutTime}:00Z` : undefined;

    // Intentar primero el flujo canónico de Spring Boot (/api/attendance/adjustments)
    const adj = await this.useCase.createAdjustment({
      attendanceRecordId: this.record.id,
      adjustmentType: this.adjustmentType,
      reason: this.reason.trim(),
      requestedValues: {
        checkInTime: fullCheckIn,
        checkOutTime: fullCheckOut
      }
    });

    if (adj) {
      this.close();
      this.regularized.emit();
      return;
    }

    // Fallback a ruta de compatibilidad /corrections si el interceptor está en modo simulado
    const ok = await this.useCase.regularize({
      attendanceRecordId: this.record.id,
      correctedCheckIn: fullCheckIn,
      correctedCheckOut: fullCheckOut,
      reason: this.reason.trim()
    });

    if (ok) {
      this.close();
      this.regularized.emit();
    }
  }

  formatTime(iso?: string | null): string {
    if (!iso) return '--:--';
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit', hour12: false });
    } catch {
      return '--:--';
    }
  }
}
