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
      await firstValueFrom(
        this.attendanceService.regularizeAttendance({
          ...request,
          reason: trimmedReason
        })
      );
      this.toastService.success('Regularización enviada exitosamente con registro inmutable de auditoría.');
      return true;
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
    reason: string
  ): Promise<boolean> {
    const trimmedReason = reason.trim();
    if (trimmedReason.length < 10) {
      this.toastService.warning('La justificación debe tener al menos 10 caracteres.');
      return false;
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
        requests.push(firstIn ? {
          attendanceRecordId,
          adjustmentType: 'TIME_CORRECTION',
          reason: trimmedReason,
          requestedValues: { punchId: firstIn.id, punchedAt }
        } : {
          attendanceRecordId,
          adjustmentType: 'MISSING_PUNCH',
          reason: trimmedReason,
          requestedValues: { punchType: 'IN', punchedAt }
        });
      }
      if (correctedCheckOut) {
        const overnight = !!correctedCheckIn && correctedCheckOut <= correctedCheckIn;
        const punchedAt = this.toInstant(workDate, correctedCheckOut, overnight);
        requests.push(lastOut ? {
          attendanceRecordId,
          adjustmentType: 'TIME_CORRECTION',
          reason: trimmedReason,
          requestedValues: { punchId: lastOut.id, punchedAt }
        } : {
          attendanceRecordId,
          adjustmentType: 'MISSING_PUNCH',
          reason: trimmedReason,
          requestedValues: { punchType: 'OUT', punchedAt }
        });
      }

      for (const request of requests) {
        await firstValueFrom(this.attendanceService.createAdjustment(request));
      }
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
