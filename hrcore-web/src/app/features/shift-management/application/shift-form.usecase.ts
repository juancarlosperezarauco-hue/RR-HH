import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ShiftService, CreateShiftRequest, ShiftResponse } from '../../../entities/shift';
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
   */
  validateLaborLaw(request: CreateShiftRequest): { isValid: boolean; message?: string } {
    const { hours, isOvernight } = this.calculateEffectiveHours(
      request.startTime,
      request.endTime,
      request.breakMinutes
    );

    // Validación de horario nocturno (máximo 7 horas continuas)
    if (request.shiftType === 'NIGHT' || isOvernight) {
      if (hours > 7) {
        return {
          isValid: false,
          message: `La jornada nocturna no puede exceder las 7 horas continuas (LGT Bolivia). Calculado: ${hours}h.`
        };
      }
    } else {
      // Jornada diurna estándar (máximo 8 horas ordinarias al día)
      if (hours > 8) {
        return {
          isValid: false,
          message: `La jornada ordinaria diaria no puede exceder las 8 horas (LGT Bolivia). Calculado: ${hours}h.`
        };
      }
    }

    // Validación de tolerancia de puntualidad (5 a 15 min)
    if (request.gracePeriodMinutes < 5 || request.gracePeriodMinutes > 15) {
      return {
        isValid: false,
        message: 'La tolerancia de puntualidad debe estar configurada entre 5 y 15 minutos.'
      };
    }

    // Validación de al menos un día laboral
    if (!request.workDays || request.workDays.length === 0) {
      return {
        isValid: false,
        message: 'Debe seleccionar al menos un día hábil de trabajo para el turno.'
      };
    }

    return { isValid: true };
  }

  async saveShift(request: CreateShiftRequest, shiftId?: string): Promise<boolean> {
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
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'Error al guardar la configuración del turno';
      this.validationError.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.saving.set(false);
    }
  }
}
