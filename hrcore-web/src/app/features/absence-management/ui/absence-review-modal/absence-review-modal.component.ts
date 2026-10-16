import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';
import { AbsenceService, LeaveAttachmentResponse, LeaveRequestResponse } from '../../../../entities/absence';
import { AbsenceReviewUseCase } from '../../application/absence-review.usecase';
import { AbsenceStatusBadgeComponent } from '../absence-status-badge/absence-status-badge.component';

@Component({
  selector: 'app-absence-review-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, AbsenceStatusBadgeComponent],
  template: `
    <div
      *ngIf="isOpen && request"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200"
    >
      <div
        class="w-full max-w-xl rounded-2xl border border-border bg-card p-6 shadow-2xl transition-all max-h-[92vh] overflow-y-auto"
        (click)="$event.stopPropagation()"
      >
        <!-- Modal Header -->
        <div class="flex items-start justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-lg font-bold text-foreground">Detalle y Evaluación de Solicitud</h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Revisión, aprobación, rechazo y justificación formal de permisos y ausencias laborales (LGT)
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

        <!-- Alerta de Segregación de Funciones (Auditoría LGT) -->
        <div
          *ngIf="isRequester && request.status === 'PENDING'"
          class="mt-4 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3.5 flex items-start gap-3 text-xs text-amber-800 dark:text-amber-300"
        >
          <lucide-icon name="shield-alert" class="h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400 mt-0.5"></lucide-icon>
          <div class="space-y-0.5">
            <p class="font-bold">Norma de Segregación de Funciones (LGT / Auditoría)</p>
            <p class="text-muted-foreground">
              Esta solicitud fue registrada por su usuario. Según las políticas de control interno de HRCore, la aprobación debe ser efectuada por otro revisor o supervisor del tenant.
            </p>
          </div>
        </div>

        <!-- Detalles de la Solicitud -->
        <div class="mt-4 space-y-4">
          <div class="rounded-xl bg-accent/40 border border-border/60 p-4 space-y-3">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-bold text-foreground">{{ request.employeeFullName || 'Colaborador' }}</p>
                <p class="text-xs font-semibold text-primary">{{ request.leaveTypeName || 'Permiso Laboral' }}</p>
              </div>
              <app-absence-status-badge [status]="request.status"></app-absence-status-badge>
            </div>

            <div class="grid grid-cols-2 gap-3 pt-2 border-t border-border/40 text-xs">
              <div>
                <span class="text-muted-foreground">Período Solicitado:</span>
                <p class="font-semibold text-foreground mt-0.5">
                  {{ request.startDate }} al {{ request.endDate }}
                </p>
                <p *ngIf="request.startTime && request.endTime" class="text-[11px] text-muted-foreground font-mono mt-0.5">
                  Horario: {{ request.startTime }} a {{ request.endTime }}
                </p>
              </div>
              <div>
                <span class="text-muted-foreground">Duración en Días Hábiles:</span>
                <p class="font-mono font-bold text-foreground mt-0.5">
                  {{ request.totalDays }} {{ request.totalDays === 1 ? 'día hábil' : 'días hábiles' }}
                </p>
              </div>
            </div>

            <div class="pt-2 border-t border-border/40 text-xs">
              <span class="text-muted-foreground">Motivo expuesto:</span>
              <p class="text-foreground mt-1 italic bg-background/60 p-2.5 rounded-xl border border-border/40">
                "{{ request.reason }}"
              </p>
            </div>

            <!-- Comentario de Revisión Previo (si fue aprobada/rechazada) -->
            <div *ngIf="request.reviewComment" class="pt-2 border-t border-border/40 text-xs">
              <span class="font-semibold" [ngClass]="request.status === 'APPROVED' ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'">
                {{ request.status === 'APPROVED' ? 'Comentario de Aprobación:' : 'Motivo del Rechazo:' }}
              </span>
              <p class="text-foreground mt-1 p-2 rounded-xl bg-background/60 border border-border/40">
                {{ request.reviewComment }}
              </p>
            </div>
          </div>

          <!-- Documentos Probatorios / Respaldo (HU15) -->
          <div class="rounded-xl border border-border bg-card p-4 space-y-2.5">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-1.5 text-xs font-bold text-foreground">
                <lucide-icon name="paperclip" class="h-4 w-4 text-primary"></lucide-icon>
                <span>Documentos de Respaldo / Justificativos ({{ attachments.length }})</span>
              </div>
              <span class="text-[11px] text-muted-foreground font-mono">Archivos probatorios</span>
            </div>

            <!-- Loading Documentos -->
            <p *ngIf="attachmentsLoading" class="text-xs text-muted-foreground py-2 flex items-center gap-1.5">
              <lucide-icon name="loader-circle" class="h-3.5 w-3.5 animate-spin text-primary"></lucide-icon>
              Cargando comprobantes...
            </p>

            <!-- Lista de Adjuntos -->
            <div *ngIf="!attachmentsLoading && attachments.length > 0" class="space-y-1.5">
              <div
                *ngFor="let attachment of attachments"
                class="flex items-center justify-between gap-2 rounded-xl border border-border/60 bg-accent/20 px-3 py-2 text-xs"
              >
                <div class="inline-flex min-w-0 items-center gap-2">
                  <lucide-icon name="file-text" class="h-4 w-4 shrink-0 text-primary"></lucide-icon>
                  <span class="font-medium text-foreground truncate max-w-[240px]">{{ attachment.filename }}</span>
                  <span class="text-[10px] text-muted-foreground font-mono">({{ formatFileSize(attachment.fileSize) }})</span>
                </div>
                <button
                  type="button"
                  (click)="downloadAttachment(attachment)"
                  class="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs font-semibold text-primary hover:bg-background transition-colors"
                  title="Descargar documento de respaldo"
                >
                  <lucide-icon name="download" class="h-3.5 w-3.5"></lucide-icon>
                  <span>Descargar</span>
                </button>
              </div>
            </div>

            <!-- Sin documentos -->
            <p *ngIf="!attachmentsLoading && attachments.length === 0" class="text-xs text-muted-foreground italic py-1">
              Esta solicitud no cuenta con documentos probatorios adjuntos.
            </p>

            <!-- Opción para Adjuntar Justificativo en Solicitudes Pendientes o Borradores -->
            <div *ngIf="canAddAttachment" class="pt-2 border-t border-border/40">
              <div class="flex items-center justify-between mb-1.5">
                <span class="text-xs font-semibold text-foreground">Justificar con nuevo documento:</span>
                <span class="text-[11px] text-muted-foreground">PDF, PNG, JPG (máx 10MB)</span>
              </div>
              <div class="flex items-center gap-2">
                <input
                  type="file"
                  (change)="onUploadProofSelected($event)"
                  accept=".pdf,.png,.jpg,.jpeg"
                  [disabled]="uploadingAttachment"
                  class="w-full rounded-xl border border-border bg-background px-3 py-1.5 text-xs text-foreground file:mr-2 file:py-1 file:px-2.5 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 transition-colors"
                />
              </div>
            </div>
          </div>

          <!-- Modo Aprobación con Comentario -->
          <div *ngIf="isApproveMode" class="space-y-2 rounded-xl border border-emerald-500/40 bg-emerald-500/5 p-4 animate-in fade-in duration-150">
            <label class="block text-xs font-bold text-emerald-700 dark:text-emerald-400">
              Comentario de Aprobación (Opcional):
            </label>
            <textarea
              [(ngModel)]="approvalComment"
              name="approvalComment"
              rows="2"
              placeholder="Indique alguna observación o acuerdo laboral (ej. Aprobado de acuerdo al cronograma de área)..."
              class="w-full rounded-xl border border-emerald-500/40 bg-background p-2.5 text-xs text-foreground placeholder:text-muted-foreground focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500"
            ></textarea>
          </div>

          <!-- Modo Rechazo: Justificación Obligatoria -->
          <div *ngIf="isRejectMode" class="space-y-2 rounded-xl border border-rose-500/40 bg-rose-500/5 p-4 animate-in fade-in duration-150">
            <div class="flex items-center justify-between">
              <label class="text-xs font-bold text-rose-600 dark:text-rose-400">
                Motivo del Rechazo <span class="text-rose-500">* (Obligatorio)</span>
              </label>
              <span
                class="text-[11px] font-mono font-medium"
                [ngClass]="rejectionReason.trim().length >= 10 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'"
              >
                {{ rejectionReason.trim().length }}/10 mín.
              </span>
            </div>
            <textarea
              [(ngModel)]="rejectionReason"
              name="rejectionReason"
              rows="3"
              placeholder="Indique con claridad las razones del rechazo (ej. Necesidad operativa crítica del área, período no consensuado)..."
              class="w-full rounded-xl border border-rose-500/50 bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-rose-500 focus:ring-1 focus:ring-rose-500"
              required
            ></textarea>
            <p *ngIf="rejectionTouched && rejectionReason.trim().length < 10" class="text-xs text-rose-500 font-medium">
              El motivo de rechazo es obligatorio (mínimo 10 caracteres).
            </p>
          </div>
        </div>

        <!-- Modal Actions -->
        <div class="mt-6 flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-border">
          <button
            type="button"
            (click)="close()"
            [disabled]="useCase.reviewing()"
            class="rounded-xl border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent transition-colors"
          >
            Cerrar
          </button>

          <!-- Acciones cuando está en estado PENDING -->
          <div *ngIf="request.status === 'PENDING'" class="flex items-center gap-2">
            <!-- Normal / Sin modo seleccionado -->
            <ng-container *ngIf="!isRejectMode && !isApproveMode">
              <button
                type="button"
                (click)="isRejectMode = true"
                [disabled]="useCase.reviewing()"
                class="inline-flex items-center gap-1.5 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-2 text-sm font-semibold text-rose-700 dark:text-rose-400 hover:bg-rose-500/20 transition-colors"
              >
                <lucide-icon name="ban" class="h-4 w-4"></lucide-icon>
                <span>Rechazar</span>
              </button>

              <button
                type="button"
                (click)="isApproveMode = true"
                [disabled]="useCase.reviewing() || isRequester"
                [title]="isRequester ? 'No puede aprobar una solicitud que usted mismo registró' : 'Evaluar y aprobar solicitud'"
                class="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-5 py-2 text-sm font-bold text-white shadow-xs hover:from-emerald-500 hover:to-teal-500 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
              >
                <lucide-icon *ngIf="useCase.reviewing()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
                <lucide-icon *ngIf="!useCase.reviewing()" name="check-circle" class="h-4 w-4"></lucide-icon>
                <span>Aprobar</span>
              </button>
            </ng-container>

            <!-- Modo Confirmar Aprobación -->
            <ng-container *ngIf="isApproveMode">
              <button
                type="button"
                (click)="isApproveMode = false"
                class="rounded-xl border border-border px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-accent"
              >
                Cancelar
              </button>
              <button
                type="button"
                (click)="onApprove()"
                [disabled]="useCase.reviewing() || isRequester"
                class="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-5 py-2 text-sm font-bold text-white shadow-xs hover:bg-emerald-500 disabled:opacity-50 transition-all"
              >
                <lucide-icon *ngIf="useCase.reviewing()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
                <span>Confirmar Aprobación</span>
              </button>
            </ng-container>

            <!-- Modo Rechazo -->
            <ng-container *ngIf="isRejectMode">
              <button
                type="button"
                (click)="isRejectMode = false"
                class="rounded-xl border border-border px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-accent"
              >
                Cancelar
              </button>
              <button
                type="button"
                (click)="onConfirmReject()"
                [disabled]="useCase.reviewing() || rejectionReason.trim().length < 10"
                class="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-5 py-2 text-sm font-bold text-white shadow-xs hover:bg-rose-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
              >
                <lucide-icon *ngIf="useCase.reviewing()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
                <span>Confirmar Rechazo</span>
              </button>
            </ng-container>
          </div>

          <!-- Si es Borrador o Aprobada, permitir Cancelar si aplica -->
          <div *ngIf="request.status === 'DRAFT' || (request.status === 'APPROVED' && canCancel)" class="flex items-center gap-2">
            <button
              type="button"
              (click)="onCancel()"
              [disabled]="useCase.reviewing()"
              class="inline-flex items-center gap-1.5 rounded-xl border border-border bg-card px-4 py-2 text-sm font-semibold text-rose-600 hover:bg-rose-500/10 transition-colors"
            >
              <lucide-icon name="x-circle" class="h-4 w-4"></lucide-icon>
              <span>Cancelar Solicitud</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AbsenceReviewModalComponent implements OnChanges {
  readonly useCase = inject(AbsenceReviewUseCase);
  private readonly absenceService = inject(AbsenceService);

  @Input() isOpen = false;
  @Input() request: LeaveRequestResponse | null = null;
  @Input() currentUserId?: string | null = null;

  @Output() closeModal = new EventEmitter<void>();
  @Output() reviewCompleted = new EventEmitter<void>();

  isApproveMode = false;
  isRejectMode = false;
  approvalComment = '';
  rejectionReason = '';
  rejectionTouched = false;
  attachments: LeaveAttachmentResponse[] = [];
  attachmentsLoading = false;
  uploadingAttachment = false;

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['isOpen'] || changes['request']) && this.isOpen && this.request) {
      this.isApproveMode = false;
      this.isRejectMode = false;
      this.approvalComment = '';
      this.rejectionReason = '';
      this.rejectionTouched = false;
      void this.loadAttachments();
    }
  }

  get isRequester(): boolean {
    if (!this.currentUserId || !this.request?.requestedBy) return false;
    return this.currentUserId.toLowerCase() === this.request.requestedBy.toLowerCase();
  }

  get canAddAttachment(): boolean {
    return this.request?.status === 'DRAFT' || this.request?.status === 'PENDING';
  }

  get canCancel(): boolean {
    if (!this.request) return false;
    if (this.request.status === 'DRAFT' || this.request.status === 'PENDING') return true;
    if (this.request.status === 'APPROVED') {
      const today = new Date().toISOString().substring(0, 10);
      return this.request.startDate >= today;
    }
    return false;
  }

  private async loadAttachments(): Promise<void> {
    if (!this.request) return;
    this.attachmentsLoading = true;
    this.attachments = [];
    try {
      const response = await firstValueFrom(this.absenceService.listAttachments(this.request.id));
      this.attachments = response.data?.content || [];
    } catch {
      this.attachments = [];
    } finally {
      this.attachmentsLoading = false;
    }
  }

  async downloadAttachment(attachment: LeaveAttachmentResponse): Promise<void> {
    if (!this.request) return;
    try {
      const blob = await firstValueFrom(
        this.absenceService.downloadAttachment(this.request.id, attachment.id)
      );
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = attachment.filename;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      // Error manejado en servicio
    }
  }

  async onUploadProofSelected(event: any): Promise<void> {
    const file = event.target.files?.[0];
    if (!file || !this.request) return;

    this.uploadingAttachment = true;
    const ok = await this.useCase.uploadJustification(this.request.id, file);
    this.uploadingAttachment = false;

    if (ok) {
      await this.loadAttachments();
      this.reviewCompleted.emit();
    }
    event.target.value = '';
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  close(): void {
    this.isApproveMode = false;
    this.isRejectMode = false;
    this.approvalComment = '';
    this.rejectionReason = '';
    this.rejectionTouched = false;
    this.attachments = [];
    this.closeModal.emit();
  }

  async onApprove(): Promise<void> {
    if (!this.request) return;
    const ok = await this.useCase.review({
      requestId: this.request.id,
      approved: true,
      rejectionReason: this.approvalComment.trim() || undefined
    });
    if (ok) {
      this.close();
      this.reviewCompleted.emit();
    }
  }

  async onConfirmReject(): Promise<void> {
    this.rejectionTouched = true;
    if (!this.request || this.rejectionReason.trim().length < 10) return;

    const ok = await this.useCase.review({
      requestId: this.request.id,
      approved: false,
      rejectionReason: this.rejectionReason.trim()
    });
    if (ok) {
      this.close();
      this.reviewCompleted.emit();
    }
  }

  async onCancel(): Promise<void> {
    if (!this.request) return;
    const ok = await this.useCase.cancel(this.request.id, this.approvalComment.trim() || undefined);
    if (ok) {
      this.close();
      this.reviewCompleted.emit();
    }
  }
}
