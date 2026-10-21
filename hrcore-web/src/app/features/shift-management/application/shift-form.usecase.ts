import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ShiftService, WorkShiftRequest } from '../../../entities/shift';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class ShiftFormUseCase {
  private readonly shiftService = inject(ShiftService);
  private readonly toastService = inject(ToastService);

  readonly saving = signal(false);
  readonly validationError = signal<string | null>(null);

  /**
   * Calcula horas efectivas considerando cruce de medianoche y tiempo de descanso
   */
  calculateEffectiveHours(startTime: string, endTime: string, breakMinutes: number): {
    hours: number;
    isOvernight: boolean;
  } {
    if (!startTime || !endTime) return { hours: 0, isOvernight: false };

    const [sh, sm] = startTime.split(':').map(Number);
    const [eh, em] = endTime.split(':').map(Number);

    if (isNaN(sh) || isNaN(sm) || isNaN(eh) || isNaN(em)) {
      return { hours: 0, isOvernight: false };
    }

    let startMinutes = sh * 60 + sm;
    let endMinutes = eh * 60 + em;
    let isOvernight = false;

    if (endMinutes <= startMinutes) {
      endMinutes += 24 * 60; // Cruce de medianoche (+1 día)
      isOvernight = true;
    }

    const totalMinutes = endMinutes - startMinutes - (breakMinutes || 0);
    const hours = Math.max(0, Number((totalMinutes / 60).toFixed(2)));
    return { hours, isOvernight };
  }

  /**
   * Valida restricciones laborales según la Ley General del Trabajo (LGT) de Bolivia
   * y las reglas temporales estrictas del backend Spring Boot
   */
  validateLaborLaw(request: WorkShiftRequest): { isValid: boolean; message?: string } {
    const { hours, isOvernight } = this.calculateEffectiveHours(
      request.startTime,
      request.endTime,
      0
    );

    // Validación de consistencia temporal (ShiftTimeRules de Spring Boot)
    const effectiveCrosses = request.crossesMidnight;
    const [sh, sm] = request.startTime.split(':').map(Number);
    const [eh, em] = request.endTime.split(':').map(Number);
    const startMins = sh * 60 + sm;
    const endMins = eh * 60 + em;

    if (!effectiveCrosses && endMins <= startMins) {
      return {
        isValid: false,
        message: 'La hora de fin debe ser posterior a la hora de inicio cuando el turno no cruza medianoche.'
      };
    }
    if (effectiveCrosses && endMins > startMins) {
      return {
        isValid: false,
        message: 'Para turnos que cruzan la medianoche, la hora de fin debe ser menor o igual a la hora de inicio (+1 día).'
      };
    }

    // El backend acepta tolerancias no negativas que no superen la duración.
    const lateTol = request.lateToleranceMinutes;
    const earlyTol = request.earlyDepartureToleranceMinutes;
    const durationMinutes = Math.round(hours * 60);
    if (lateTol < 0 || earlyTol < 0 || lateTol > durationMinutes || earlyTol > durationMinutes) {
      return {
        isValid: false,
        message: 'Las tolerancias deben ser no negativas y no superar la duración de la jornada.'
      };
    }

    return { isValid: true };
  }

  async saveShift(request: WorkShiftRequest, shiftId?: string): Promise<boolean> {
    const val = this.validateLaborLaw(request);
    if (!val.isValid) {
      this.validationError.set(val.message!);
      this.toastService.error(val.message!);
      return false;
    }

    this.saving.set(true);
    this.validationError.set(null);
    try {
      if (shiftId) {
        await firstValueFrom(this.shiftService.updateShift(shiftId, request));
        this.toastService.success('Turno actualizado conforme a normativa legal');
      } else {
        await firstValueFrom(this.shiftService.createShift(request));
        this.toastService.success('Turno configurado y registrado exitosamente');
      }
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string; errors?: string[] } };
      const msg = errorResponse?.error?.errors?.[0] || errorResponse?.error?.message || 'Error al guardar la configuración del turno';
      this.validationError.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.saving.set(false);
    }
  }
}
