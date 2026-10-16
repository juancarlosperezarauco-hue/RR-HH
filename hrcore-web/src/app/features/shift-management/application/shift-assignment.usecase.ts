import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  ShiftService,
  EmployeeShiftAssignmentResponse,
  AssignShiftRequest,
  CreateShiftAssignmentRequest
} from '../../../entities/shift';
import { ToastService } from '../../../shared/ui/toast/toast.service';
import { EmployeesService, EmployeeSummary } from '../../../entities/employees';
import { ShiftResponse } from '../../../entities/shift';

@Injectable({ providedIn: 'root' })
export class ShiftAssignmentUseCase {
  private readonly shiftService = inject(ShiftService);
  private readonly employeesService = inject(EmployeesService);
  private readonly toastService = inject(ToastService);

  readonly assignments = signal<EmployeeShiftAssignmentResponse[]>([]);
  readonly loading = signal(false);
  readonly assigning = signal(false);
  readonly error = signal<string | null>(null);

  async loadAssignments(): Promise<void> {
    this.loading.set(true);
    try {
      // 1. Intentar listado canónico de Spring Boot: /api/shifts/assignments
      const [res, employeesRes, shiftsRes] = await Promise.all([
        firstValueFrom(this.shiftService.listAssignments()),
        firstValueFrom(this.employeesService.list(0, 100)).catch(() => null),
        firstValueFrom(this.shiftService.listShifts('ALL', 0, 100)).catch(() => null)
      ]);
      if (res?.data?.content) {
        const employees = new Map<string, EmployeeSummary>(
          (employeesRes?.data?.content || []).map((employee) => [employee.id, employee])
        );
        const shifts = new Map<string, ShiftResponse>(
          (shiftsRes?.data?.content || []).map((shift) => [shift.id, shift])
        );
        this.assignments.set(res.data.content.map((assignment) => {
          const employee = employees.get(assignment.employeeId);
          const shift = shifts.get(assignment.shiftId);
          return {
            ...assignment,
            employeeFullName: employee ? `${employee.firstName} ${employee.lastName}`.trim() : undefined,
            employeeCode: employee?.employeeNumber,
            shiftName: shift?.name,
            isRecurring: (assignment.weekdays?.length || 0) > 0
          };
        }));
        return;
      }
    } catch {
      // Fallback a ruta legada
    }

    try {
      const res = await firstValueFrom(this.shiftService.getEmployeeAssignments('all'));
      this.assignments.set(res.data);
    } catch {
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

  private extractErrorMessage(err: unknown): string {
    const errorObj = err as any;
    const apiError = errorObj?.error;

    let rawMessage = '';
    if (apiError) {
      if (Array.isArray(apiError.errors) && apiError.errors.length > 0) {
        rawMessage = apiError.errors.join('. ');
      } else if (typeof apiError.message === 'string' && apiError.message.trim().length > 0) {
        rawMessage = apiError.message;
      }
    } else if (typeof errorObj?.message === 'string') {
      rawMessage = errorObj.message;
    }

    if (!rawMessage) {
      return 'Error al asignar turno al colaborador';
    }

    if (rawMessage.includes('startDate cannot precede today')) {
      return 'La fecha de inicio no puede ser anterior a la fecha de hoy.';
    }
    if (rawMessage.includes('endDate cannot precede startDate')) {
      return 'La fecha fin no puede ser anterior a la fecha de inicio.';
    }
    if (rawMessage.includes('company.timezone')) {
      return 'Configure la zona horaria en la configuración de la empresa antes de asignar turnos.';
    }
    if (rawMessage.includes('Employee must be active')) {
      return 'El colaborador seleccionado debe estar activo.';
    }

    return rawMessage;
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

    const weekdays = request.weekdays && request.weekdays.length > 0 ? request.weekdays : [1, 2, 3, 4, 5];
    const errors: string[] = [];
    const newAssignments: EmployeeShiftAssignmentResponse[] = [];

    try {
      for (const empId of request.employeeIds) {
        const payload: CreateShiftAssignmentRequest = {
          employeeId: empId,
          shiftId: request.shiftId,
          startDate: request.startDate,
          endDate: request.endDate ? request.endDate : null,
          weekdays
        };

        try {
          const res = await firstValueFrom(this.shiftService.createAssignment(payload));
          if (res?.data) {
            newAssignments.push(res.data);
          }
        } catch (err: unknown) {
          const detail = this.extractErrorMessage(err);
          errors.push(detail);
        }
      }

      if (newAssignments.length > 0) {
        this.assignments.update((prev) => [...newAssignments, ...prev]);
        this.loadAssignments().catch(() => {});
      }

      if (errors.length === 0) {
        this.toastService.success(
          `Turno asignado exitosamente a ${newAssignments.length} colaborador(es)`
        );
        return true;
      } else {
        const uniqueErrors = Array.from(new Set(errors));
        const errorMessage = uniqueErrors.join('. ');
        this.error.set(errorMessage);
        this.toastService.error(errorMessage);
        return newAssignments.length > 0;
      }
    } catch (err: unknown) {
      const msg = this.extractErrorMessage(err);
      this.error.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.assigning.set(false);
    }
  }

  /**
   * Cancelar asignación de turno
   * PATCH /api/shifts/assignments/{id}/cancel
   */
  async cancelAssignment(id: string): Promise<boolean> {
    try {
      await firstValueFrom(this.shiftService.cancelAssignment(id));
      this.toastService.success('Asignación cancelada exitosamente');
      this.assignments.update((list) =>
        list.map((a) => (a.id === id ? { ...a, status: 'CANCELLED' } : a))
      );
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'Error al cancelar la asignación';
      this.toastService.error(msg);
      return false;
    }
  }

  /**
   * Cerrar asignación de turno
   * PATCH /api/shifts/assignments/{id}/close
   */
  async closeAssignment(id: string): Promise<boolean> {
    try {
      await firstValueFrom(this.shiftService.closeAssignment(id));
      this.toastService.success('Asignación cerrada exitosamente');
      this.assignments.update((list) =>
        list.map((a) => (a.id === id ? { ...a, status: 'INACTIVE' } : a))
      );
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const msg = errorResponse?.error?.message || 'Error al cerrar la asignación';
      this.toastService.error(msg);
      return false;
    }
  }
}
