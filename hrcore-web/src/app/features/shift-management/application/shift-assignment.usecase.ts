import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  ShiftService,
  EmployeeShiftAssignmentResponse,
  AssignShiftRequest
} from '../../../entities/shift';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class ShiftAssignmentUseCase {
  private readonly shiftService = inject(ShiftService);
  private readonly toastService = inject(ToastService);

  readonly assignments = signal<EmployeeShiftAssignmentResponse[]>([]);
  readonly loading = signal(false);
  readonly assigning = signal(false);
  readonly error = signal<string | null>(null);

  async loadAssignments(): Promise<void> {
    this.loading.set(true);
    try {
      // Obtenemos asignaciones generales simuladas o del backend
      const res = await firstValueFrom(this.shiftService.getEmployeeAssignments('all'));
      this.assignments.set(res.data);
    } catch {
      // Fallback a asignaciones precargadas
      this.assignments.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * Verifica solapamiento de fechas con turnos activos asignados al colaborador
   */
  validateOverlap(
    employeeId: string,
    startDate: string,
    endDate?: string | null
  ): { hasOverlap: boolean; message?: string } {
    if (!startDate) return { hasOverlap: false };

    const newStart = new Date(startDate).getTime();
    const newEnd = endDate ? new Date(endDate).getTime() : Infinity;

    const existingList = this.assignments().filter(
      (a) => a.employeeId === employeeId && a.status === 'ACTIVE'
    );

    for (const ex of existingList) {
      const exStart = new Date(ex.startDate).getTime();
      const exEnd = ex.endDate ? new Date(ex.endDate).getTime() : Infinity;

      // Condición estándar de solapamiento de intervalos de fecha
      if (newStart <= exEnd && newEnd >= exStart) {
        return {
          hasOverlap: true,
          message: `El colaborador ya tiene el turno "${ex.shiftName || 'Turno Vigente'}" asignado en ese rango de fechas (${ex.startDate} a ${ex.endDate || 'Indefinido'}).`
        };
      }
    }

    return { hasOverlap: false };
  }

  async assign(request: AssignShiftRequest): Promise<boolean> {
    // Validar solapamiento para cada colaborador seleccionado
    for (const empId of request.employeeIds) {
      const overlapCheck = this.validateOverlap(empId, request.startDate, request.endDate);
      if (overlapCheck.hasOverlap) {
        this.error.set(overlapCheck.message!);
        this.toastService.error(overlapCheck.message!);
        return false;
      }
    }

    this.assigning.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(this.shiftService.assignShifts(request));
      this.toastService.success('Turno asignado exitosamente sin solapamientos');
      // Actualizar asignaciones locales
      this.assignments.update((prev) => [...res.data, ...prev]);
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'Error al asignar el turno a los trabajadores';
      this.error.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.assigning.set(false);
    }
  }
}
