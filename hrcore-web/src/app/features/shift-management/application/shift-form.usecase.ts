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
   * y las reglas temporales estrictas del backend Spring Boot
   */
  validateLaborLaw(request: CreateShiftRequest): { isValid: boolean; message?: string } {
    const { hours, isOvernight } = this.calculateEffectiveHours(
      request.startTime,
      request.endTime,
      request.breakMinutes
    );

    // Validación de consistencia temporal (ShiftTimeRules de Spring Boot)
    const effectiveCrosses = request.crossesMidnight ?? isOvernight;
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

    // Validación de horario nocturno (máximo 7 horas continuas)
    if (request.shiftType === 'NIGHT' || effectiveCrosses) {
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
    const lateTol = request.lateToleranceMinutes ?? request.gracePeriodMinutes ?? 10;
    if (lateTol < 5 || lateTol > 15) {
      return {
        isValid: false,
        message: 'La tolerancia de llegada tarde debe estar configurada entre 5 y 15 minutos.'
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

    // Sincronizar campos de Spring Boot
    const { isOvernight } = this.calculateEffectiveHours(request.startTime, request.endTime, request.breakMinutes);
    request.crossesMidnight = request.crossesMidnight ?? isOvernight;
    request.isOvernight = request.crossesMidnight;
    request.lateToleranceMinutes = request.lateToleranceMinutes ?? request.gracePeriodMinutes ?? 10;
    request.earlyDepartureToleranceMinutes = request.earlyDepartureToleranceMinutes ?? 5;

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
