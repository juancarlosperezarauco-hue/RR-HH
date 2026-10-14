import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AbsenceService, CreateLeaveRequestPayload } from '../../../entities/absence';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AbsenceRequestUseCase {
  private readonly absenceService = inject(AbsenceService);
  private readonly toastService = inject(ToastService);

  readonly submitting = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  /**
   * Cómputo exclusivo de días hábiles (Lunes a Viernes) y exclusión opcional de feriados oficiales
   */
  calculateBusinessDays(startStr: string, endStr: string, holidays: string[] = []): number {
    if (!startStr || !endStr) return 0;
    const start = new Date(startStr + 'T00:00:00');
    const end = new Date(endStr + 'T00:00:00');
    if (end < start) return 0;

    let count = 0;
    const cur = new Date(start);
    while (cur <= end) {
      const day = cur.getDay();
      const dateIso = cur.toISOString().substring(0, 10);
      const isWeekend = day === 0 || day === 6; // Domingo (0) o Sábado (6)
      const isHoliday = holidays.includes(dateIso);

      if (!isWeekend && !isHoliday) {
        count++;
      }
      cur.setDate(cur.getDate() + 1);
    }
    return count;
  }

  async submitRequest(
    payload: CreateLeaveRequestPayload,
    isVacation: boolean,
    remainingDays?: number,
    requiresAttachment?: boolean,
    holidays: string[] = []
  ): Promise<boolean> {
    this.error.set(null);

    if (payload.endDate < payload.startDate) {
      const msg = 'La fecha de fin no puede ser anterior a la fecha de inicio';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    const neededDays = this.calculateBusinessDays(payload.startDate, payload.endDate, holidays);
    if (neededDays <= 0) {
      const msg = 'El período seleccionado no incluye ningún día hábil laborable.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    if (isVacation && remainingDays !== undefined) {
      if (neededDays > remainingDays) {
        const msg = `Los días solicitados (${neededDays}) superan el saldo disponible (${remainingDays} días).`;
        this.error.set(msg);
        this.toastService.warning(msg);
        return false;
      }
    }

    if (requiresAttachment && !payload.proofDocument) {
      const msg = 'Este tipo de ausencia requiere adjuntar un documento probatorio obligatorio.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    this.submitting.set(true);
    try {
      const draftRes = await firstValueFrom(
        this.absenceService.createRequest({
          employeeId: payload.employeeId,
          leaveTypeId: payload.leaveTypeId,
          startDate: payload.startDate,
          endDate: payload.endDate,
          reason: payload.reason
        })
      );
      const requestId = draftRes.data.id;

      if (payload.proofDocument) {
        await firstValueFrom(this.absenceService.uploadAttachment(requestId, payload.proofDocument));
      }

      await firstValueFrom(this.absenceService.submitRequestDraft(requestId));
      this.toastService.success('Solicitud de ausencia registrada correctamente para revisión.');
      return true;
    } catch (err: any) {
      const msg = err?.error?.message || err?.message || 'Error al enviar la solicitud';
      this.error.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.submitting.set(false);
    }
  }
}
