import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AttendanceService, RegularizeAttendanceRequest } from '../../../entities/attendance';
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
}
