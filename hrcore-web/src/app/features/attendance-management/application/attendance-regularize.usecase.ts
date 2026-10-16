import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AttendanceService,
  CreateAdjustmentRequest,
  AttendanceAdjustmentResponse,
  AttendancePunchResponse,
  AttendanceStatus,
  PunchType
} from '../../../entities/attendance';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class AttendanceRegularizeUseCase {
  private readonly attendanceService = inject(AttendanceService);
  private readonly toastService = inject(ToastService);

  readonly submitting = signal<boolean>(false);
  readonly loadingHistory = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  /**
   * Carga el historial de correcciones/ajustes de una jornada específica
   */
  async loadAdjustments(recordId: string): Promise<AttendanceAdjustmentResponse[]> {
    this.loadingHistory.set(true);
    try {
      const res = await firstValueFrom(
        this.attendanceService.listAdjustments({ recordId, page: 0, size: 50, sort: 'createdAt', direction: 'desc' })
      );
      return res.data?.content || [];
    } catch {
      return [];
    } finally {
      this.loadingHistory.set(false);
    }
  }

  /**
   * Carga las marcaciones individuales asociadas a una jornada
   */
  async loadPunches(recordId: string): Promise<AttendancePunchResponse[]> {
    try {
      const res = await firstValueFrom(
        this.attendanceService.listPunches({ recordId, page: 0, size: 50, sort: 'punchedAt', direction: 'asc' })
      );
      return res.data?.content || [];
    } catch {
      return [];
    }
  }

  /**
   * Crea una solicitud justificada de ajuste
   */
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
      this.toastService.success('Ajuste de asistencia solicitado y registrado en auditoría.');
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

  /**
   * Corrección de horario sobre una marcación existente (TIME_CORRECTION)
   */
  async submitTimeCorrection(
    attendanceRecordId: string,
    punchId: string,
    punchedAt: string,
    reason: string
  ): Promise<boolean> {
    const result = await this.createAdjustment({
      attendanceRecordId,
      adjustmentType: 'TIME_CORRECTION',
      reason,
      requestedValues: {
        punchId,
        punchedAt
      }
    });
    return !!result;
  }

  /**
   * Registro de marcación omitida/olvidada (MISSING_PUNCH)
   */
  async submitMissingPunch(
    attendanceRecordId: string,
    punchType: PunchType,
    punchedAt: string,
    reason: string
  ): Promise<boolean> {
    const result = await this.createAdjustment({
      attendanceRecordId,
      adjustmentType: 'MISSING_PUNCH',
      reason,
      requestedValues: {
        punchType,
        punchedAt
      }
    });
    return !!result;
  }

  /**
   * Corrección del estado de la jornada (STATUS_CORRECTION)
   */
  async submitStatusCorrection(
    attendanceRecordId: string,
    status: AttendanceStatus,
    reason: string
  ): Promise<boolean> {
    const result = await this.createAdjustment({
      attendanceRecordId,
      adjustmentType: 'STATUS_CORRECTION',
      reason,
      requestedValues: {
        status
      }
    });
    return !!result;
  }

  /**
   * Justificación por otro motivo legal / justificado (OTHER)
   */
  async submitOtherJustified(
    attendanceRecordId: string,
    reason: string
  ): Promise<boolean> {
    const result = await this.createAdjustment({
      attendanceRecordId,
      adjustmentType: 'OTHER',
      reason,
      requestedValues: {
        status: 'JUSTIFIED'
      }
    });
    return !!result;
  }

  /**
   * Aprobación de ajuste por revisor/supervisor
   */
  async approve(id: string, comment?: string): Promise<boolean> {
    this.submitting.set(true);
    try {
      await firstValueFrom(this.attendanceService.approveAdjustment(id, { comment: comment?.trim() || undefined }));
      this.toastService.success('Ajuste de asistencia aprobado y aplicado exitosamente.');
      return true;
    } catch (err: any) {
      this.toastService.error(err?.error?.message || 'Error al aprobar el ajuste');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  /**
   * Rechazo justificado de ajuste
   */
  async reject(id: string, comment: string): Promise<boolean> {
    if (!comment || comment.trim().length < 5) {
      this.toastService.warning('El motivo de rechazo debe contener al menos 5 caracteres.');
      return false;
    }
    this.submitting.set(true);
    try {
      await firstValueFrom(this.attendanceService.rejectAdjustment(id, { comment: comment.trim() }));
      this.toastService.success('Ajuste de asistencia rechazado.');
      return true;
    } catch (err: any) {
      this.toastService.error(err?.error?.message || 'Error al rechazar el ajuste');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  /**
   * Cancelación de ajuste por el solicitante
   */
  async cancel(id: string): Promise<boolean> {
    this.submitting.set(true);
    try {
      await firstValueFrom(this.attendanceService.cancelAdjustment(id));
      this.toastService.success('Solicitud de ajuste cancelada.');
      return true;
    } catch (err: any) {
      this.toastService.error(err?.error?.message || 'Error al cancelar la solicitud');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }

  /**
   * Recalcula la jornada contra los turnos asignados
   */
  async recalculateRecord(employeeId: string, workDate: string): Promise<boolean> {
    this.submitting.set(true);
    try {
      await firstValueFrom(this.attendanceService.calculateRecord({ employeeId, workDate }));
      this.toastService.success('Jornada recalculada con éxito según turnos asignados.');
      return true;
    } catch (err: any) {
      this.toastService.error(err?.error?.message || 'Error al recalcular la jornada');
      return false;
    } finally {
      this.submitting.set(false);
    }
  }
}
