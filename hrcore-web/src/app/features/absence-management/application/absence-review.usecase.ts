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
        try {
          await firstValueFrom(
            this.absenceService.approveRequest(payload.requestId, payload.rejectionReason)
          );
        } catch {
          await firstValueFrom(this.absenceService.reviewRequest(payload));
        }
      } else {
        try {
          await firstValueFrom(
            this.absenceService.rejectRequest(payload.requestId, payload.rejectionReason)
          );
        } catch {
          await firstValueFrom(this.absenceService.reviewRequest(payload));
        }
      }

      const msg = payload.approved
        ? 'Solicitud aprobada exitosamente y sincronizada con el control de asistencia.'
        : 'Solicitud rechazada con justificación auditada.';
      this.toastService.success(msg);
      return true;
    } catch (err: any) {
      const msg = err?.error?.message || err?.message || 'Error al procesar la revisión de la solicitud';
      this.error.set(msg);
      this.toastService.error(msg);
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
      this.toastService.error(err?.error?.message || 'Error al cancelar la solicitud');
      return false;
    } finally {
      this.reviewing.set(false);
    }
  }
}
