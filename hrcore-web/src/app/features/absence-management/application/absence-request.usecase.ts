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
    holidays: string[] = [],
    deductsBalance?: boolean
  ): Promise<boolean> {
    this.error.set(null);

    if (!payload.employeeId) {
      const msg = 'Debe seleccionar un colaborador / trabajador.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    if (!payload.leaveTypeId) {
      const msg = 'Debe seleccionar el tipo de permiso o ausencia.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    if (payload.endDate < payload.startDate) {
      const msg = 'La fecha de fin no puede ser anterior a la fecha de inicio.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    if (payload.startTime && payload.endTime && payload.endTime <= payload.startTime) {
      const msg = 'La hora de fin debe ser posterior a la hora de inicio en permisos por horas.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    const neededDays = this.calculateBusinessDays(payload.startDate, payload.endDate, holidays);
    if (neededDays <= 0 && !payload.startTime) {
      const msg = 'El período seleccionado no incluye ningún día hábil laborable.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    if (requiresAttachment && !payload.proofDocument) {
      const msg = 'Este tipo de permiso requiere adjuntar obligatoriamente un documento probatorio de respaldo.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    this.submitting.set(true);
    try {
      // 1. Si es vacación o descuenta saldo, garantizar que el balance anual esté inicializado
      if (isVacation || deductsBalance) {
        const year = new Date(payload.startDate + 'T00:00:00').getFullYear();
        try {
          const balRes = await firstValueFrom(this.absenceService.getEmployeeBalance(payload.employeeId, year));
          const hasBalance = balRes.data && balRes.data.openingDays != null && balRes.data.openingDays > 0;
          if (!hasBalance) {
            await firstValueFrom(this.absenceService.createBalance({
              employeeId: payload.employeeId,
              leaveTypeId: payload.leaveTypeId,
              balanceYear: year,
              openingDays: 15
            })).catch(() => null);
          }
        } catch {
          await firstValueFrom(this.absenceService.createBalance({
            employeeId: payload.employeeId,
            leaveTypeId: payload.leaveTypeId,
            balanceYear: year,
            openingDays: 15
          })).catch(() => null);
        }
      }

      // Formatear tiempos con precisión de segundos si se enviaron horas
      const startTimeFormatted = payload.startTime
        ? (payload.startTime.length === 5 ? `${payload.startTime}:00` : payload.startTime)
        : null;
      const endTimeFormatted = payload.endTime
        ? (payload.endTime.length === 5 ? `${payload.endTime}:00` : payload.endTime)
        : null;

      // 2. Crear borrador de solicitud
      const draftRes = await firstValueFrom(
        this.absenceService.createRequest({
          employeeId: payload.employeeId,
          leaveTypeId: payload.leaveTypeId,
          startDate: payload.startDate,
          endDate: payload.endDate,
          startTime: startTimeFormatted,
          endTime: endTimeFormatted,
          reason: payload.reason.trim()
        })
      );
      const requestId = draftRes.data.id;

      // 3. Subir archivo adjunto probatorio si fue proporcionado
      if (payload.proofDocument) {
        await firstValueFrom(this.absenceService.uploadAttachment(requestId, payload.proofDocument));
      }

      // 4. Enviar solicitud para revisión formal
      await firstValueFrom(this.absenceService.submitRequestDraft(requestId));
      this.toastService.success('Solicitud de ausencia o permiso registrada y enviada correctamente.');
      return true;
    } catch (err: any) {
      let rawMsg = err?.error?.message || err?.message || 'Error al procesar la solicitud de ausencia';
      let friendlyMsg = rawMsg;

      if (rawMsg.includes('Leave has no chargeable working days')) {
        friendlyMsg = 'El trabajador no tiene un turno de trabajo asignado en las fechas seleccionadas o el período no contiene días laborables.';
      } else if (rawMsg.includes('Annual leave balance must be initialized')) {
        friendlyMsg = 'El saldo de vacación anual para este trabajador debe ser inicializado antes de registrar vacaciones.';
      } else if (rawMsg.includes('Insufficient uncommitted leave balance') || rawMsg.includes('Insufficient leave balance')) {
        friendlyMsg = 'Los días solicitados superan el saldo disponible de vacaciones del colaborador.';
      } else if (rawMsg.includes('Overlapping leave request exists')) {
        friendlyMsg = 'Ya existe otra solicitud de permiso o ausencia registrada para este trabajador en el mismo rango de fechas.';
      } else if (rawMsg.includes('Unsupported attachment content') || rawMsg.includes('Filename, MIME type and content do not match')) {
        friendlyMsg = 'El archivo de respaldo no es válido. Solo se admiten archivos PDF, PNG o JPG.';
      } else if (rawMsg.includes('Partial leave requires one day')) {
        friendlyMsg = 'Los permisos por horas deben registrarse para un único día (misma fecha de inicio y fin).';
      }

      this.error.set(friendlyMsg);
      this.toastService.error(friendlyMsg);
      return false;
    } finally {
      this.submitting.set(false);
    }
  }
}
