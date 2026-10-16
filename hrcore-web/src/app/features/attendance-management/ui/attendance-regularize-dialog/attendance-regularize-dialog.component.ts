import {
  Component,
  Input,
  Output,
  EventEmitter,
  inject,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import {
  AttendanceRecordResponse,
  AdjustmentType,
  AttendanceStatus,
  AttendanceAdjustmentResponse,
  AttendancePunchResponse,
  PunchType
} from '../../../../entities/attendance';
import { AttendanceRegularizeUseCase } from '../../application/attendance-regularize.usecase';
import { AttendanceStatusBadgeComponent } from '../attendance-status-badge/attendance-status-badge.component';

@Component({
  selector: 'app-attendance-regularize-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, AttendanceStatusBadgeComponent],
  template: `
    <div
      *ngIf="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200"
    >
      <div
        class="w-full max-w-2xl max-h-[92vh] flex flex-col rounded-2xl border border-border bg-card shadow-2xl transition-all overflow-hidden"
        (click)="$event.stopPropagation()"
      >
        <!-- Modal Header -->
        <div class="flex items-start justify-between border-b border-border p-5 shrink-0 bg-muted/20">
          <div>
            <div class="flex items-center gap-2">
              <h3 class="text-lg font-bold text-foreground">Gestión y Corrección de Asistencia</h3>
              <span class="rounded-md bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                CU17
              </span>
            </div>
            <p class="text-xs text-muted-foreground mt-0.5">
              Auditoría laboral inmutable y regularización justificada conforme a ley
            </p>
          </div>
          <button
            type="button"
            (click)="close()"
            class="rounded-xl p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
          >
            <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
          </button>
        </div>

        <!-- Colaborador y Resumen de Jornada -->
        <div *ngIf="record" class="p-4 bg-muted/30 border-b border-border shrink-0">
          <div class="flex flex-wrap items-center justify-between gap-3 text-xs">
            <div>
              <span class="font-bold text-foreground text-sm block">
                {{ record.employeeFullName || 'Colaborador' }}
              </span>
              <span class="font-mono text-muted-foreground text-[11px]">
                Código: {{ record.employeeCode || record.employeeId }}
              </span>
            </div>

            <div class="flex items-center gap-2">
              <app-attendance-status-badge
                [status]="record.status"
                [lateMinutes]="record.lateMinutes"
                [checkOutTime]="record.checkOutTime"
              ></app-attendance-status-badge>

              <button
                type="button"
                (click)="onRecalculate()"
                [disabled]="useCase.submitting()"
                class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-2.5 py-1 text-xs font-semibold text-foreground hover:bg-accent transition-colors shadow-xs"
                title="Recalcular jornada con respecto a los turnos asignados"
              >
                <lucide-icon name="refresh-ccw" class="h-3.5 w-3.5" [ngClass]="{ 'animate-spin': useCase.submitting() }"></lucide-icon>
                <span>Recalcular</span>
              </button>
            </div>
          </div>

          <div class="mt-3 grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-border/50 text-xs">
            <div>
              <span class="text-muted-foreground block text-[10px] uppercase font-bold">Fecha</span>
              <span class="font-semibold text-foreground">{{ record.workDate }}</span>
            </div>
            <div>
              <span class="text-muted-foreground block text-[10px] uppercase font-bold">Entrada Real</span>
              <span class="font-mono font-medium text-foreground">{{ formatTime(record.checkInTime) }}</span>
            </div>
            <div>
              <span class="text-muted-foreground block text-[10px] uppercase font-bold">Salida Real</span>
              <span class="font-mono font-medium text-foreground">{{ formatTime(record.checkOutTime) }}</span>
            </div>
            <div>
              <span class="text-muted-foreground block text-[10px] uppercase font-bold">Horas Computadas</span>
              <span class="font-semibold text-foreground">{{ record.workedHours ?? 0 }}h efectivas</span>
            </div>
          </div>
        </div>

        <!-- Navigation Tabs -->
        <div class="flex border-b border-border bg-card px-4 shrink-0">
          <button
            type="button"
            (click)="activeTab = 'punches'"
            class="px-4 py-2.5 text-xs font-bold border-b-2 transition-colors flex items-center gap-1.5"
            [ngClass]="activeTab === 'punches' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'"
          >
            <lucide-icon name="clock" class="h-3.5 w-3.5"></lucide-icon>
            Marcaciones del Día ({{ punches.length }})
          </button>
          <button
            type="button"
            (click)="activeTab = 'history'"
            class="px-4 py-2.5 text-xs font-bold border-b-2 transition-colors flex items-center gap-1.5"
            [ngClass]="activeTab === 'history' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'"
          >
            <lucide-icon name="clipboard-list" class="h-3.5 w-3.5"></lucide-icon>
            Historial de Correcciones ({{ adjustments.length }})
          </button>
          <button
            type="button"
            (click)="activeTab = 'new'"
            class="px-4 py-2.5 text-xs font-bold border-b-2 transition-colors flex items-center gap-1.5"
            [ngClass]="activeTab === 'new' ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'"
          >
            <lucide-icon name="plus" class="h-3.5 w-3.5"></lucide-icon>
            Nueva Corrección Justificada
          </button>
        </div>

        <!-- Tab Contents (Scrollable) -->
        <div class="p-5 overflow-y-auto flex-1">
          <!-- TAB 1: Marcaciones del Día -->
          <div *ngIf="activeTab === 'punches'" class="space-y-3">
            <div *ngIf="punches.length === 0" class="text-center py-8 text-muted-foreground">
              <lucide-icon name="clock" class="h-8 w-8 mx-auto mb-2 opacity-50"></lucide-icon>
              <p class="font-medium text-sm text-foreground">Sin marcaciones registradas para esta jornada</p>
              <p class="text-xs mt-1">El colaborador no tiene registros biométricos ni web en la fecha indicada.</p>
            </div>

            <div *ngFor="let punch of punches" class="flex items-center justify-between rounded-xl border border-border p-3.5 hover:bg-accent/20 transition-colors">
              <div class="flex items-center gap-3">
                <div
                  class="flex h-9 w-9 items-center justify-center rounded-xl text-white font-bold text-xs shadow-xs"
                  [ngClass]="getPunchBadgeColor(punch.punchType)"
                >
                  <lucide-icon [name]="getPunchIcon(punch.punchType)" class="h-4 w-4"></lucide-icon>
                </div>
                <div>
                  <div class="font-semibold text-foreground text-sm flex items-center gap-2">
                    <span>{{ getPunchTypeLabel(punch.punchType) }}</span>
                    <span class="rounded bg-muted px-1.5 py-0.5 text-[10px] font-mono text-muted-foreground border border-border uppercase">
                      {{ punch.source }}
                    </span>
                  </div>
                  <div class="text-xs text-muted-foreground mt-0.5">
                    Registrado a las: <strong class="font-mono text-foreground">{{ formatFullDateTime(punch.punchedAt) }}</strong>
                  </div>
                  <div *ngIf="punch.notes" class="text-xs text-muted-foreground italic mt-1">
                    Nota: "{{ punch.notes }}"
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- TAB 2: Historial de Correcciones -->
          <div *ngIf="activeTab === 'history'" class="space-y-4">
            <div *ngIf="adjustments.length === 0" class="text-center py-8 text-muted-foreground">
              <lucide-icon name="shield-check" class="h-8 w-8 mx-auto mb-2 opacity-50 text-emerald-500"></lucide-icon>
              <p class="font-medium text-sm text-foreground">Sin correcciones registradas</p>
              <p class="text-xs mt-1">Esta jornada no posee solicitudes de ajuste o regularización previas.</p>
            </div>

            <div *ngFor="let adj of adjustments" class="rounded-xl border border-border p-4 bg-card shadow-xs space-y-3">
              <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border/60 pb-2">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-xs px-2 py-0.5 rounded-md bg-primary/10 text-primary">
                    {{ getAdjustmentTypeLabel(adj.adjustmentType) }}
                  </span>
                  <span class="text-[11px] text-muted-foreground">
                    {{ formatFullDateTime(adj.createdAt) }}
                  </span>
                </div>
                <div>
                  <span
                    class="px-2.5 py-0.5 rounded-full text-xs font-bold border"
                    [ngClass]="getReviewStatusBadge(adj.status)"
                  >
                    {{ getReviewStatusLabel(adj.status) }}
                  </span>
                </div>
              </div>

              <!-- Motivo -->
              <div class="text-xs">
                <span class="font-bold text-foreground">Motivo justificado:</span>
                <p class="text-muted-foreground mt-0.5 bg-muted/30 p-2.5 rounded-lg border border-border/40">
                  {{ adj.reason }}
                </p>
              </div>

              <!-- Comentario de Revisión si existe -->
              <div *ngIf="adj.reviewComment" class="text-xs bg-accent/30 p-2.5 rounded-lg border border-border/60">
                <span class="font-bold text-foreground">Comentario de Revisión:</span>
                <p class="text-muted-foreground mt-0.5">{{ adj.reviewComment }}</p>
                <span class="text-[10px] text-muted-foreground mt-1 block font-mono">
                  Revisado el: {{ formatFullDateTime(adj.reviewedAt) }}
                </span>
              </div>

              <!-- Acciones sobre correcciones PENDING -->
              <div *ngIf="adj.status === 'PENDING'" class="flex flex-wrap items-center justify-end gap-2 pt-2 border-t border-border/40">
                <!-- Botón Cancelar -->
                <button
                  type="button"
                  (click)="onCancelAdjustment(adj.id)"
                  [disabled]="useCase.submitting()"
                  class="rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                >
                  Cancelar Solicitud
                </button>

                <!-- Botón Rechazar -->
                <button
                  type="button"
                  (click)="onRejectAdjustment(adj.id)"
                  [disabled]="useCase.submitting()"
                  class="rounded-lg bg-rose-500/10 border border-rose-500/30 px-3 py-1.5 text-xs font-semibold text-rose-700 dark:text-rose-400 hover:bg-rose-500/20 transition-colors"
                >
                  Rechazar
                </button>

                <!-- Botón Aprobar -->
                <button
                  type="button"
                  (click)="onApproveAdjustment(adj.id)"
                  [disabled]="useCase.submitting()"
                  class="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-emerald-500 transition-colors shadow-xs"
                >
                  Aprobar Ajuste
                </button>
              </div>
            </div>
          </div>

          <!-- TAB 3: Nueva Corrección Justificada -->
          <div *ngIf="activeTab === 'new'" class="space-y-4">
            <!-- Aviso si ya hay una corrección pendiente -->
            <div *ngIf="hasPendingAdjustment" class="rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-xs text-amber-800 dark:text-amber-300 space-y-1">
              <div class="flex items-center gap-2 font-bold">
                <lucide-icon name="alert-triangle" class="h-4 w-4 shrink-0"></lucide-icon>
                <span>Ajuste pendiente en curso</span>
              </div>
              <p>
                Esta jornada ya tiene una solicitud de ajuste pendiente de revisión. Para mantener la integridad de la auditoría laboral, debe resolverse (aprobarse, rechazarse o cancelarse) antes de ingresar una nueva.
              </p>
            </div>

            <form *ngIf="!hasPendingAdjustment" (ngSubmit)="onSubmitAdjustment()" class="space-y-4">
              <!-- Selector de Tipo de Ajuste -->
              <div>
                <label class="block text-xs font-semibold text-foreground mb-1">
                  Tipo de Corrección Justificada <span class="text-rose-500">*</span>
                </label>
                <select
                  [(ngModel)]="adjustmentType"
                  name="adjustmentType"
                  (ngModelChange)="onAdjustmentTypeChange()"
                  class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                >
                  <option value="TIME_CORRECTION">Corregir Hora de Marcación Existente</option>
                  <option value="MISSING_PUNCH">Registrar Marcación Omitida / Olvidada</option>
                  <option value="STATUS_CORRECTION">Corregir Estado de la Jornada</option>
                  <option value="OTHER">Justificar Jornada / Otro Motivo</option>
                </select>
              </div>

              <!-- OPCIÓN 1: TIME_CORRECTION -->
              <div *ngIf="adjustmentType === 'TIME_CORRECTION'" class="space-y-3 rounded-xl border border-border p-3.5 bg-muted/20">
                <div>
                  <label class="block text-xs font-semibold text-foreground mb-1">
                    Seleccione Marcación a Corregir <span class="text-rose-500">*</span>
                  </label>
                  <select
                    [(ngModel)]="selectedPunchId"
                    name="selectedPunchId"
                    class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                  >
                    <option value="" disabled>Seleccione una marcación</option>
                    <option *ngFor="let p of punches" [value]="p.id">
                      {{ getPunchTypeLabel(p.punchType) }} — Registrada a las {{ formatTime(p.punchedAt) }}
                    </option>
                  </select>
                </div>

                <div>
                  <label class="block text-xs font-semibold text-foreground mb-1">
                    Hora Corregida <span class="text-rose-500">*</span>
                  </label>
                  <input
                    type="time"
                    [(ngModel)]="correctedTime"
                    name="correctedTime"
                    required
                    class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <!-- OPCIÓN 2: MISSING_PUNCH -->
              <div *ngIf="adjustmentType === 'MISSING_PUNCH'" class="space-y-3 rounded-xl border border-border p-3.5 bg-muted/20">
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-xs font-semibold text-foreground mb-1">
                      Tipo de Marcación Faltante <span class="text-rose-500">*</span>
                    </label>
                    <select
                      [(ngModel)]="missingPunchType"
                      name="missingPunchType"
                      class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                    >
                      <option value="IN">Entrada (IN)</option>
                      <option value="OUT">Salida (OUT)</option>
                      <option value="BREAK_START">Inicio de Pausa</option>
                      <option value="BREAK_END">Fin de Pausa / Retorno</option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-xs font-semibold text-foreground mb-1">
                      Hora de la Marcación <span class="text-rose-500">*</span>
                    </label>
                    <input
                      type="time"
                      [(ngModel)]="correctedTime"
                      name="missingCorrectedTime"
                      required
                      class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                  </div>
                </div>
              </div>

              <!-- OPCIÓN 3: STATUS_CORRECTION -->
              <div *ngIf="adjustmentType === 'STATUS_CORRECTION'" class="space-y-3 rounded-xl border border-border p-3.5 bg-muted/20">
                <div>
                  <label class="block text-xs font-semibold text-foreground mb-1">
                    Nuevo Estado de Asistencia <span class="text-rose-500">*</span>
                  </label>
                  <select
                    [(ngModel)]="requestedStatus"
                    name="requestedStatus"
                    class="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                  >
                    <option value="PRESENT">Presente / Puntual</option>
                    <option value="LATE">Con Retraso</option>
                    <option value="JUSTIFIED">Ausencia Justificada</option>
                    <option value="DAY_OFF">Día de Descanso</option>
                    <option value="ABSENT">Inasistencia / Falta</option>
                  </select>
                </div>
              </div>

              <!-- OPCIÓN 4: OTHER -->
              <div *ngIf="adjustmentType === 'OTHER'" class="rounded-xl border border-sky-500/30 bg-sky-500/10 p-3 text-xs text-sky-800 dark:text-sky-300">
                <p>
                  Esta opción regulariza la jornada como <strong>Ausencia Justificada (JUSTIFIED)</strong> mediante respaldo o justificación administrativa.
                </p>
              </div>

              <!-- Justificación Obligatoria -->
              <div>
                <div class="flex items-center justify-between mb-1">
                  <label class="text-xs font-semibold text-foreground">
                    Motivo / Justificación Obligatoria <span class="text-rose-500">*</span>
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
                  placeholder="Detalle el motivo laboral y normativo de este ajuste (ej. Falla en reloj biométrico confirmada por supervisor, comisión externa autorizada...)"
                  class="w-full rounded-xl border border-border bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:ring-1 focus:ring-primary"
                  required
                ></textarea>
                <p *ngIf="reasonTouched && reason.trim().length < 10" class="mt-1 text-xs text-rose-500 font-medium flex items-center gap-1">
                  <lucide-icon name="alert-circle" class="h-3.5 w-3.5"></lucide-icon>
                  La justificación debe tener al menos 10 caracteres.
                </p>
              </div>

              <!-- Aviso legal auditoría -->
              <div class="rounded-xl border border-primary/20 bg-primary/5 p-3 flex items-start gap-2.5 text-xs text-primary">
                <lucide-icon name="shield-check" class="h-4 w-4 shrink-0 mt-0.5"></lucide-icon>
                <p class="leading-relaxed">
                  <strong>Trazabilidad Legal:</strong> Toda corrección queda registrada con usuario, fecha, motivo y valores anteriores para fiscalización de horas trabajadas y puntualidad (LGT Bolivia).
                </p>
              </div>

              <!-- Botón Submit -->
              <div class="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  (click)="close()"
                  [disabled]="useCase.submitting()"
                  class="rounded-xl border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent disabled:opacity-50 transition-colors"
                >
                  Cerrar
                </button>
                <button
                  type="submit"
                  [disabled]="useCase.submitting() || reason.trim().length < 10"
                  class="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2 text-sm font-bold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-xs"
                >
                  <lucide-icon *ngIf="useCase.submitting()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
                  <lucide-icon *ngIf="!useCase.submitting()" name="check-circle" class="h-4 w-4"></lucide-icon>
                  <span>Registrar Corrección Justificada</span>
                </button>
              </div>
            </form>
          </div>
        </div>

      </div>
    </div>
  `
})
export class AttendanceRegularizeDialogComponent implements OnChanges {
  readonly useCase = inject(AttendanceRegularizeUseCase);

  @Input() isOpen = false;
  @Input() record: AttendanceRecordResponse | null = null;
  @Output() closeDialog = new EventEmitter<void>();
  @Output() regularized = new EventEmitter<void>();

  activeTab: 'punches' | 'history' | 'new' = 'punches';

  punches: AttendancePunchResponse[] = [];
  adjustments: AttendanceAdjustmentResponse[] = [];

  // Formulario de Ajuste
  adjustmentType: AdjustmentType = 'TIME_CORRECTION';
  selectedPunchId = '';
  missingPunchType: PunchType = 'IN';
  correctedTime = '08:30';
  requestedStatus: AttendanceStatus = 'PRESENT';
  reason = '';
  reasonTouched = false;

  get hasPendingAdjustment(): boolean {
    return this.adjustments.some((a) => a.status === 'PENDING');
  }

  async ngOnChanges(changes: SimpleChanges): Promise<void> {
    if (changes['isOpen'] && this.isOpen && this.record) {
      await this.loadData();
    }
  }

  async loadData(): Promise<void> {
    if (!this.record) return;
    this.reason = '';
    this.reasonTouched = false;

    const [punches, adjustments] = await Promise.all([
      this.useCase.loadPunches(this.record.id),
      this.useCase.loadAdjustments(this.record.id)
    ]);
    this.punches = punches;
    this.adjustments = adjustments;

    if (this.punches.length > 0) {
      this.selectedPunchId = this.punches[0].id;
    } else {
      this.selectedPunchId = '';
      if (this.adjustmentType === 'TIME_CORRECTION') {
        this.adjustmentType = 'MISSING_PUNCH';
      }
    }
  }

  onAdjustmentTypeChange(): void {
    if (this.adjustmentType === 'TIME_CORRECTION' && this.punches.length === 0) {
      this.adjustmentType = 'MISSING_PUNCH';
    }
  }

  close(): void {
    this.closeDialog.emit();
  }

  async onRecalculate(): Promise<void> {
    if (!this.record) return;
    const ok = await this.useCase.recalculateRecord(this.record.employeeId, this.record.workDate);
    if (ok) {
      await this.loadData();
      this.regularized.emit();
    }
  }

  async onSubmitAdjustment(): Promise<void> {
    this.reasonTouched = true;
    if (!this.record || this.reason.trim().length < 10) return;

    let success = false;
    const workDate = this.record.workDate;
    const timeIso = this.toIso(workDate, this.correctedTime);

    if (this.adjustmentType === 'TIME_CORRECTION') {
      if (!this.selectedPunchId) {
        this.selectedPunchId = this.punches[0]?.id || '';
      }
      if (!this.selectedPunchId) return;
      success = await this.useCase.submitTimeCorrection(
        this.record.id,
        this.selectedPunchId,
        timeIso,
        this.reason.trim()
      );
    } else if (this.adjustmentType === 'MISSING_PUNCH') {
      success = await this.useCase.submitMissingPunch(
        this.record.id,
        this.missingPunchType,
        timeIso,
        this.reason.trim()
      );
    } else if (this.adjustmentType === 'STATUS_CORRECTION') {
      success = await this.useCase.submitStatusCorrection(
        this.record.id,
        this.requestedStatus,
        this.reason.trim()
      );
    } else if (this.adjustmentType === 'OTHER') {
      success = await this.useCase.submitOtherJustified(
        this.record.id,
        this.reason.trim()
      );
    }

    if (success) {
      this.reason = '';
      this.reasonTouched = false;
      await this.loadData();
      this.activeTab = 'history';
      this.regularized.emit();
    }
  }

  async onApproveAdjustment(id: string): Promise<void> {
    const comment = prompt('Ingrese comentario de aprobación (opcional):') ?? undefined;
    const ok = await this.useCase.approve(id, comment);
    if (ok) {
      await this.loadData();
      this.regularized.emit();
    }
  }

  async onRejectAdjustment(id: string): Promise<void> {
    const comment = prompt('Ingrese motivo de rechazo (obligatorio, mín. 5 caracteres):');
    if (!comment) return;
    const ok = await this.useCase.reject(id, comment);
    if (ok) {
      await this.loadData();
      this.regularized.emit();
    }
  }

  async onCancelAdjustment(id: string): Promise<void> {
    const confirmed = confirm('¿Está seguro de cancelar esta solicitud de ajuste?');
    if (!confirmed) return;
    const ok = await this.useCase.cancel(id);
    if (ok) {
      await this.loadData();
      this.regularized.emit();
    }
  }

  private toIso(workDate: string, time: string): string {
    const t = time.length === 5 ? `${time}:00` : time;
    const date = new Date(`${workDate}T${t}`);
    return date.toISOString();
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

  formatFullDateTime(iso?: string | null): string {
    if (!iso) return '--';
    try {
      const d = new Date(iso);
      return `${d.toLocaleDateString('es-BO')} ${d.toLocaleTimeString('es-BO', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })}`;
    } catch {
      return '--';
    }
  }

  getPunchTypeLabel(type: PunchType): string {
    switch (type) {
      case 'IN': return 'Entrada';
      case 'OUT': return 'Salida';
      case 'BREAK_START':
      case 'BREAK_OUT': return 'Inicio de Pausa';
      case 'BREAK_END':
      case 'BREAK_IN': return 'Retorno de Pausa';
      default: return type;
    }
  }

  getPunchIcon(type: PunchType): string {
    switch (type) {
      case 'IN': return 'log-in';
      case 'OUT': return 'log-out';
      case 'BREAK_START':
      case 'BREAK_OUT': return 'coffee';
      case 'BREAK_END':
      case 'BREAK_IN': return 'play';
      default: return 'clock';
    }
  }

  getPunchBadgeColor(type: PunchType): string {
    switch (type) {
      case 'IN': return 'bg-emerald-600';
      case 'OUT': return 'bg-rose-600';
      case 'BREAK_START':
      case 'BREAK_OUT': return 'bg-amber-600';
      case 'BREAK_END':
      case 'BREAK_IN': return 'bg-purple-600';
      default: return 'bg-slate-600';
    }
  }

  getAdjustmentTypeLabel(type: AdjustmentType): string {
    switch (type) {
      case 'TIME_CORRECTION': return 'Corrección de Horario';
      case 'MISSING_PUNCH': return 'Marcación Omitida';
      case 'STATUS_CORRECTION': return 'Corrección de Estado';
      case 'OTHER': return 'Otro Motivo / Justificado';
      default: return type;
    }
  }

  getReviewStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'Pendiente';
      case 'APPROVED': return 'Aprobado';
      case 'REJECTED': return 'Rechazado';
      case 'CANCELLED': return 'Cancelado';
      default: return status;
    }
  }

  getReviewStatusBadge(status: string): string {
    switch (status) {
      case 'PENDING': return 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/30';
      case 'APPROVED': return 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/30';
      case 'REJECTED': return 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/30';
      case 'CANCELLED': return 'bg-muted text-muted-foreground border-border';
      default: return 'bg-muted text-muted-foreground border-border';
    }
  }
}
