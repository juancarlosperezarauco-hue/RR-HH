import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AbsenceService, ReviewLeaveRequestPayload } from '../../../entities/absence';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AbsenceReviewUseCase {
  private readonly absenceService = inject(AbsenceService);
  private readonly toastService = inject(ToastService);

  readonly reviewing = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  async review(payload: ReviewLeaveRequestPayload): Promise<boolean> {
    if (!payload.approved) {
      const reason = (payload.rejectionReason || '').trim();
      if (reason.length < 10) {
        const msg = 'El motivo de rechazo es obligatorio (mínimo 10 caracteres).';
        this.error.set(msg);
        this.toastService.warning(msg);
        return false;
      }
    }

    this.reviewing.set(true);
    this.error.set(null);

    try {
      if (payload.approved) {
        await firstValueFrom(
          this.absenceService.approveRequest(payload.requestId, payload.rejectionReason)
        );
      } else {
        await firstValueFrom(
          this.absenceService.rejectRequest(payload.requestId, payload.rejectionReason)
        );
      }

      const msg = payload.approved
        ? 'Solicitud aprobada exitosamente y sincronizada con el control de asistencia.'
        : 'Solicitud rechazada con justificación auditada.';
      this.toastService.success(msg);
      return true;
    } catch (err: any) {
      let rawMsg = err?.error?.message || err?.message || 'Error al procesar la revisión de la solicitud';
      let friendlyMsg = rawMsg;

      if (rawMsg.includes('Requester cannot approve their own leave request')) {
        friendlyMsg = 'No puede aprobar una solicitud registrada por usted mismo. Por normativa de auditoría laboral (LGT), la aprobación debe realizarla otro supervisor o revisor.';
      } else if (rawMsg.includes('Only pending requests can be approved')) {
        friendlyMsg = 'Solo las solicitudes en estado Pendiente pueden ser aprobadas.';
      } else if (rawMsg.includes('Only pending requests can be rejected')) {
        friendlyMsg = 'Solo las solicitudes en estado Pendiente pueden ser rechazadas.';
      } else if (rawMsg.includes('Insufficient leave balance')) {
        friendlyMsg = 'El colaborador no cuenta con saldo de vacación suficiente para aprobar esta solicitud.';
      }

      this.error.set(friendlyMsg);
      this.toastService.error(friendlyMsg);
      return false;
    } finally {
      this.reviewing.set(false);
    }
  }

  async cancel(requestId: string, comment?: string): Promise<boolean> {
    this.reviewing.set(true);
    try {
      await firstValueFrom(this.absenceService.cancelRequest(requestId, comment));
      this.toastService.success('Solicitud cancelada correctamente.');
      return true;
    } catch (err: any) {
      const msg = err?.error?.message || 'Error al cancelar la solicitud';
      this.toastService.error(msg);
      return false;
    } finally {
      this.reviewing.set(false);
    }
  }

  async uploadJustification(requestId: string, file: File): Promise<boolean> {
    this.reviewing.set(true);
    try {
      await firstValueFrom(this.absenceService.uploadAttachment(requestId, file));
      this.toastService.success('Documento de respaldo / justificativo adjuntado correctamente.');
      return true;
    } catch (err: any) {
      const msg = err?.error?.message || 'Error al adjuntar el documento de respaldo';
      this.toastService.error(msg);
      return false;
    } finally {
      this.reviewing.set(false);
    }
  }
}
