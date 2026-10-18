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
        class="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-2xl transition-all"
        (click)="$event.stopPropagation()"
      >
        <!-- Modal Header -->
        <div class="flex items-start justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-lg font-bold text-foreground">Revisión de Solicitud de Ausencia</h3>
            <p class="text-xs text-muted-foreground mt-0.5">
              Decisión formal de RRHH / Supervisión con auditoría laboral
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

        <!-- Detalles de la Solicitud -->
        <div class="mt-4 space-y-3">
          <div class="rounded-xl bg-accent/40 border border-border/60 p-4 space-y-2">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-bold text-foreground">{{ request.employeeFullName || 'Colaborador' }}</p>
                <p class="text-xs font-semibold text-primary">{{ request.leaveTypeName }}</p>
              </div>
              <app-absence-status-badge [status]="request.status"></app-absence-status-badge>
            </div>

            <div class="grid grid-cols-2 gap-2 pt-2 border-t border-border/40 text-xs">
              <div>
                <span class="text-muted-foreground">Período Solicitado:</span>
                <p class="font-medium text-foreground mt-0.5">{{ request.startDate }} al {{ request.endDate }}</p>
              </div>
              <div>
                <span class="text-muted-foreground">Días Hábiles:</span>
                <p class="font-mono font-bold text-foreground mt-0.5">{{ request.totalDays }} días</p>
              </div>
            </div>

            <div class="pt-2 text-xs">
              <span class="text-muted-foreground">Motivo expuesto:</span>
              <p class="text-foreground mt-0.5 italic bg-background/50 p-2 rounded-lg border border-border/30">
                "{{ request.reason }}"
              </p>
            </div>

            <!-- Documentos probatorios privados de la solicitud -->
            <div class="pt-2 border-t border-border/40">
              <p class="text-xs font-semibold text-foreground mb-1.5">Documentos probatorios</p>
              <p *ngIf="attachmentsLoading" class="text-xs text-muted-foreground">Cargando documentos...</p>
              <p *ngIf="!attachmentsLoading && attachments.length === 0" class="text-xs text-muted-foreground">
                La solicitud no tiene documentos adjuntos.
              </p>
              <button
                *ngFor="let attachment of attachments"
                type="button"
                (click)="downloadAttachment(attachment)"
                class="flex w-full items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-left text-xs text-primary hover:bg-background/70"
              >
                <span class="inline-flex min-w-0 items-center gap-1.5">
                  <lucide-icon name="file-text" class="h-3.5 w-3.5 shrink-0"></lucide-icon>
                  <span class="truncate">{{ attachment.filename }}</span>
                </span>
                <lucide-icon name="download" class="h-3.5 w-3.5 shrink-0"></lucide-icon>
              </button>
            </div>
          </div>

          <!-- Modo Rechazo: Justificación Obligatoria -->
          <div *ngIf="isRejectMode" class="space-y-2 pt-2 animate-in fade-in duration-200">
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
              placeholder="Indique con claridad las razones del rechazo (ej. Necesidad operativa crítica del área, período no consensuado...)"
              class="w-full rounded-xl border border-rose-500/50 bg-background p-3 text-sm text-foreground placeholder:text-muted-foreground focus:border-rose-500 focus:ring-1 focus:ring-rose-500"
              required
            ></textarea>
            <p *ngIf="rejectionTouched && rejectionReason.trim().length < 10" class="text-xs text-rose-500 font-medium">
              El motivo de rechazo es obligatorio (mínimo 10 caracteres).
            </p>
          </div>
        </div>

        <!-- Modal Actions -->
        <div class="mt-6 flex items-center justify-between gap-3 pt-3 border-t border-border">
          <button
            type="button"
            (click)="close()"
            [disabled]="useCase.reviewing()"
            class="rounded-xl border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent transition-colors"
          >
            Cerrar
          </button>

          <div class="flex items-center gap-2">
            <!-- Si no está en modo rechazo -->
            <ng-container *ngIf="!isRejectMode">
              <button
                type="button"
                (click)="isRejectMode = true"
                [disabled]="useCase.reviewing() || request.status !== 'PENDING'"
                class="inline-flex items-center gap-1.5 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-2 text-sm font-semibold text-rose-700 dark:text-rose-400 hover:bg-rose-500/20 disabled:opacity-50 transition-colors"
              >
                <lucide-icon name="ban" class="h-4 w-4"></lucide-icon>
                Rechazar
              </button>

              <button
                type="button"
                (click)="onApprove()"
                [disabled]="useCase.reviewing() || request.status !== 'PENDING'"
                class="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-5 py-2 text-sm font-bold text-white shadow-xs hover:from-emerald-500 hover:to-teal-500 disabled:opacity-50 transition-all"
              >
                <lucide-icon *ngIf="useCase.reviewing()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
                <lucide-icon *ngIf="!useCase.reviewing()" name="check-circle" class="h-4 w-4"></lucide-icon>
                Aprobar
              </button>
            </ng-container>

            <!-- Si está en modo rechazo -->
            <ng-container *ngIf="isRejectMode">
              <button
                type="button"
                (click)="isRejectMode = false"
                class="rounded-xl border border-border px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-accent"
              >
                Cancelar Rechazo
              </button>

              <button
                type="button"
                (click)="onConfirmReject()"
                [disabled]="useCase.reviewing() || rejectionReason.trim().length < 10"
                class="inline-flex items-center gap-1.5 rounded-xl bg-rose-600 px-5 py-2 text-sm font-bold text-white shadow-xs hover:bg-rose-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
              >
                <lucide-icon *ngIf="useCase.reviewing()" name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon>
                Confirmar Rechazo
              </button>
            </ng-container>
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
  @Output() closeModal = new EventEmitter<void>();
  @Output() reviewCompleted = new EventEmitter<void>();

  isRejectMode = false;
  rejectionReason = '';
  rejectionTouched = false;
  attachments: LeaveAttachmentResponse[] = [];
  attachmentsLoading = false;

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['isOpen'] || changes['request']) && this.isOpen && this.request) {
      void this.loadAttachments();
    }
  }

  private async loadAttachments(): Promise<void> {
    if (!this.request) return;
    this.attachmentsLoading = true;
    this.attachments = [];
    try {
      const response = await firstValueFrom(this.absenceService.listAttachments(this.request.id));
      this.attachments = response.data.content ?? [];
    } catch {
      this.attachments = [];
    } finally {
      this.attachmentsLoading = false;
    }
  }

  async downloadAttachment(attachment: LeaveAttachmentResponse): Promise<void> {
    if (!this.request) return;
    const blob = await firstValueFrom(
      this.absenceService.downloadAttachment(this.request.id, attachment.id)
    );
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = attachment.filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  close(): void {
    this.isRejectMode = false;
    this.rejectionReason = '';
    this.rejectionTouched = false;
    this.attachments = [];
    this.closeModal.emit();
  }

  async onApprove(): Promise<void> {
    if (!this.request) return;
    const ok = await this.useCase.review({
      requestId: this.request.id,
      approved: true
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
}
