import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ShiftService, EmployeeShiftAssignmentResponse, AssignShiftRequest } from '../../../entities/shift';
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

    const weekdays = request.weekdays && request.weekdays.length > 0 ? request.weekdays : [1, 2, 3, 4, 5];

    try {
      const res = await firstValueFrom(this.shiftService.assignShifts({ ...request, weekdays }));
      this.toastService.success(`Turno asignado a ${res.data.length} colaborador(es) exitosamente`);
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
