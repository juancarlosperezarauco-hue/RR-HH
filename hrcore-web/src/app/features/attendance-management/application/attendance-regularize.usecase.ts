import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AttendanceService,
  RegularizeAttendanceRequest,
  CreateAdjustmentRequest,
  AttendanceAdjustmentResponse
} from '../../../entities/attendance';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AttendanceRegularizeUseCase {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toastService = inject(ToastService);

  readonly submitting = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  async regularize(request: RegularizeAttendanceRequest): Promise<boolean> {
    const trimmedReason = (request.reason || '').trim();
    if (trimmedReason.length < 10) {
      const msg = 'La justificación es obligatoria y debe tener al menos 10 caracteres.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return false;
    }

    this.submitting.set(true);
    this.error.set(null);

    try {
      const record = await firstValueFrom(this.attendanceService.getRecordById(request.attendanceRecordId));
      return await this.regularizeTimes(
        request.attendanceRecordId,
        record.data.workDate,
        request.correctedCheckIn,
        request.correctedCheckOut,
        trimmedReason
      );
    } catch (err: any) {
      const msg = err?.error?.message || err?.message || 'Error al procesar la regularización.';
      this.error.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  async createAdjustment(request: CreateAdjustmentRequest): Promise<AttendanceAdjustmentResponse | null> {
    const trimmedReason = (request.reason || '').trim();
    if (trimmedReason.length < 10) {
      const msg = 'La justificación es obligatoria y debe tener al menos 10 caracteres.';
      this.error.set(msg);
      this.toastService.warning(msg);
      return null;
    }

    this.submitting.set(true);
    this.error.set(null);

    try {
      const res = await firstValueFrom(
        this.attendanceService.createAdjustment({
          ...request,
          reason: trimmedReason
        })
      );
      this.toastService.success('Ajuste de asistencia solicitado correctamente.');
      return res.data;
    } catch (err: any) {
      const msg = err?.error?.message || err?.message || 'Error al solicitar el ajuste.';
      this.error.set(msg);
      this.toastService.error(msg);
      return null;
    } finally {
      this.submitting.set(false);
    }
  }

  async regularizeTimes(
    attendanceRecordId: string,
    workDate: string,
    correctedCheckIn: string | undefined,
    correctedCheckOut: string | undefined,
    reason: string,
    adjustmentType: 'TIME_CORRECTION' | 'MISSING_PUNCH' | 'STATUS_CORRECTION' | 'OTHER' = 'TIME_CORRECTION',
    status: string = 'JUSTIFIED'
  ): Promise<boolean> {
    const trimmedReason = reason.trim();
    if (trimmedReason.length < 10) {
      this.toastService.warning('La justificación debe tener al menos 10 caracteres.');
      return false;
    }
    if (adjustmentType === 'TIME_CORRECTION' && (!!correctedCheckIn === !!correctedCheckOut)) {
      this.toastService.warning(correctedCheckIn && correctedCheckOut
        ? 'Solicite una sola corrección por vez: entrada o salida.'
        : 'Ingrese la hora de entrada o la hora de salida que desea corregir.');
      return false;
    }
    if (adjustmentType === 'MISSING_PUNCH' && (!!correctedCheckIn === !!correctedCheckOut)) {
      this.toastService.warning('Para una marcación omitida indique una sola hora.');
      return false;
    }
    if (adjustmentType === 'STATUS_CORRECTION' || adjustmentType === 'OTHER') {
      try {
        await firstValueFrom(this.attendanceService.createAdjustment({
          attendanceRecordId, adjustmentType, reason: trimmedReason,
          requestedValues: { status: adjustmentType === 'OTHER' ? 'JUSTIFIED' : status }
        }));
        this.toastService.success('Regularización enviada para revisión.');
        return true;
      } catch (err: any) {
        const message = err?.error?.message || err?.message || 'No se pudo solicitar la regularización.';
        this.error.set(message); this.toastService.error(message); return false;
      } finally { this.submitting.set(false); }
    }
    if (!correctedCheckIn && !correctedCheckOut) {
      this.toastService.warning('Ingrese al menos una hora corregida.');
      return false;
    }

    this.submitting.set(true);
    this.error.set(null);
    try {
      const punchesResponse = await firstValueFrom(
        this.attendanceService.listPunches({ recordId: attendanceRecordId, page: 0, size: 100 })
      );
      const punches = punchesResponse.data?.content || [];
      const firstIn = punches
        .filter((punch) => punch.punchType === 'IN')
        .sort((a, b) => a.punchedAt.localeCompare(b.punchedAt))[0];
      const lastOut = punches
        .filter((punch) => punch.punchType === 'OUT')
        .sort((a, b) => b.punchedAt.localeCompare(a.punchedAt))[0];

      const requests: CreateAdjustmentRequest[] = [];
      if (correctedCheckIn) {
        const punchedAt = this.toInstant(workDate, correctedCheckIn, false);
        requests.push(adjustmentType === 'TIME_CORRECTION' && firstIn ? {
          attendanceRecordId,
          adjustmentType,
          reason: trimmedReason,
          requestedValues: { punchId: firstIn.id, punchedAt }
        } : {
          attendanceRecordId,
          adjustmentType,
          reason: trimmedReason,
          requestedValues: { punchType: 'IN', punchedAt }
        });
      }
      if (correctedCheckOut) {
        const overnight = !!correctedCheckIn && correctedCheckOut <= correctedCheckIn;
        const punchedAt = this.toInstant(workDate, correctedCheckOut, overnight);
        requests.push(adjustmentType === 'TIME_CORRECTION' && lastOut ? {
          attendanceRecordId,
          adjustmentType,
          reason: trimmedReason,
          requestedValues: { punchId: lastOut.id, punchedAt }
        } : {
          attendanceRecordId,
          adjustmentType,
          reason: trimmedReason,
          requestedValues: { punchType: 'OUT', punchedAt }
        });
      }

      await firstValueFrom(this.attendanceService.createAdjustment(requests[0]));
      this.toastService.success('Regularización enviada para revisión con trazabilidad de auditoría.');
      return true;
    } catch (err: any) {
      const message = err?.error?.message || err?.message || 'No se pudo solicitar la regularización.';
      this.error.set(message);
      this.toastService.error(message);
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  private toInstant(workDate: string, time: string, followingDay: boolean): string {
    const date = new Date(`${workDate}T${time}:00`);
    if (followingDay) date.setDate(date.getDate() + 1);
    return date.toISOString();
  }

  async approve(id: string, comment?: string): Promise<boolean> {
    this.submitting.set(true);
    try {
      await firstValueFrom(this.attendanceService.approveAdjustment(id, { comment }));
      this.toastService.success('Ajuste de asistencia aprobado correctamente.');
      return true;
    } catch (err: any) {
      this.toastService.error(err?.error?.message || 'Error al aprobar el ajuste');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  async reject(id: string, comment?: string): Promise<boolean> {
    if (!comment || comment.trim().length < 5) {
      this.toastService.warning('El motivo de rechazo debe contener al menos 5 caracteres.');
      return false;
    }
    this.submitting.set(true);
    try {
      await firstValueFrom(this.attendanceService.rejectAdjustment(id, { comment }));
      this.toastService.success('Ajuste de asistencia rechazado.');
      return true;
    } catch (err: any) {
      this.toastService.error(err?.error?.message || 'Error al rechazar el ajuste');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }
}
