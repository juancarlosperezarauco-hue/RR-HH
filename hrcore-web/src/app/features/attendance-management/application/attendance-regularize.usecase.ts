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
