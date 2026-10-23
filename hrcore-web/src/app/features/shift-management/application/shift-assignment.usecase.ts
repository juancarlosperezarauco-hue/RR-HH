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
      const rawAssignments: EmployeeShiftAssignmentResponse[] = Array.isArray(res?.data)
        ? (res.data as EmployeeShiftAssignmentResponse[])
        : (res?.data?.content || []);

      const rawEmployees: EmployeeSummary[] = Array.isArray(employeesRes?.data)
        ? (employeesRes.data as EmployeeSummary[])
        : (employeesRes?.data?.content || []);

      const rawShifts: ShiftResponse[] = Array.isArray(shiftsRes?.data)
        ? (shiftsRes.data as ShiftResponse[])
        : (shiftsRes?.data?.content || []);

      const employees = new Map<string, EmployeeSummary>(
        rawEmployees.map((employee) => [employee.id, employee])
      );
      const shifts = new Map<string, ShiftResponse>(
        rawShifts.map((shift) => [shift.id, shift])
      );

      this.assignments.set(rawAssignments.map((assignment) => {
        const employee = employees.get(assignment.employeeId);
        const shift = shifts.get(assignment.shiftId);
        return {
          ...assignment,
          employeeFullName: employee ? `${employee.firstName} ${employee.lastName}`.trim() : undefined,
          employeeCode: employee?.employeeNumber,
          shiftName: shift?.name,
          shiftStartTime: shift?.startTime,
          shiftEndTime: shift?.endTime,
          shiftCrossesMidnight: shift?.crossesMidnight
        };
      }));
      return;
    } catch (err: unknown) {
      this.assignments.set([]);
      const errorResponse = err as { error?: { message?: string; errors?: string[] } };
      this.error.set(errorResponse?.error?.errors?.[0] || errorResponse?.error?.message || 'No se pudieron cargar las asignaciones de turnos');
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
    endDate?: string | null,
    requestedShift?: Pick<AssignShiftRequest, 'weekdays' | 'shiftStartTime' | 'shiftEndTime' | 'shiftCrossesMidnight'>
  ): { hasOverlap: boolean; message?: string } {
    if (!startDate) return { hasOverlap: false };

    const newStart = this.epochDay(startDate);
    const newEnd = endDate ? this.epochDay(endDate) : Infinity;

    const existingList = this.assignments().filter(
      (a) => a.employeeId === employeeId && a.status === 'ACTIVE'
    );

    for (const ex of existingList) {
      const exStart = this.epochDay(ex.startDate);
      const exEnd = ex.endDate ? this.epochDay(ex.endDate) : Infinity;

      if (!(newStart <= exEnd && newEnd >= exStart)) continue;

      // Si no se conocen los horarios (por ejemplo, datos antiguos de la tabla),
      // conservamos una prevalidación conservadora. El backend sigue siendo la
      // autoridad final y valida el conflicto dentro de la misma transacción.
      if (!requestedShift?.weekdays?.length || !requestedShift.shiftStartTime || !requestedShift.shiftEndTime) {
        return {
          hasOverlap: true,
          message: `El colaborador ya tiene el turno "${ex.shiftName || 'Turno Vigente'}" asignado en ese rango de fechas (${ex.startDate} a ${ex.endDate || 'Indefinido'}).`
        };
      }

      // Si la asignación existente no pudo enriquecerse con su turno, no
      // bloqueamos a ciegas: la política transaccional del backend decidirá.
      if (!ex.weekdays?.length || !ex.shiftStartTime || !ex.shiftEndTime) continue;

      if (this.hasScheduleConflict(
        requestedShift.weekdays,
        requestedShift.shiftStartTime,
        requestedShift.shiftEndTime,
        requestedShift.shiftCrossesMidnight ?? false,
        newStart,
        newEnd,
        ex.weekdays,
        ex.shiftStartTime,
        ex.shiftEndTime,
        ex.shiftCrossesMidnight ?? false,
        exStart,
        exEnd
      )) {
        return {
          hasOverlap: true,
          message: `El colaborador ya tiene el turno "${ex.shiftName || 'Turno Vigente'}" asignado en un horario que se solapa (${ex.startDate} a ${ex.endDate || 'Indefinido'}).`
        };
      }
    }

    return { hasOverlap: false };
  }

  async assign(request: AssignShiftRequest): Promise<boolean> {
    // Validar solapamiento para cada colaborador seleccionado
    for (const empId of request.employeeIds) {
      const overlapCheck = this.validateOverlap(empId, request.startDate, request.endDate, request);
      if (overlapCheck.hasOverlap) {
        this.error.set(overlapCheck.message!);
        this.toastService.error(overlapCheck.message!);
        return false;
      }
    }

    this.assigning.set(true);
    this.error.set(null);

    const weekdays = [...new Set(request.weekdays ?? [])].filter((day) => day >= 1 && day <= 7).sort((a, b) => a - b);
    if (weekdays.length === 0) {
      const message = 'Selecciona al menos un día de aplicación para la asignación.';
      this.error.set(message);
      this.toastService.error(message);
      return false;
    }

    try {
      const { shiftStartTime, shiftEndTime, shiftCrossesMidnight, ...apiRequest } = request;
      const res = await firstValueFrom(this.shiftService.assignShifts({ ...apiRequest, weekdays }));
      this.toastService.success(`Turno asignado a ${res.data.length} colaborador(es) exitosamente`);
      this.assignments.update((prev) => [...res.data, ...prev]);
      return true;
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string; errors?: string[] } };
      const msg = errorResponse?.error?.errors?.[0] || errorResponse?.error?.message || 'Error al asignar el turno a los trabajadores';
      this.error.set(msg);
      this.toastService.error(msg);
      return false;
    } finally {
      this.assigning.set(false);
    }
  }

  private epochDay(value: string): number {
    const [year, month, day] = value.split('-').map(Number);
    return Math.floor(Date.UTC(year, month - 1, day) / 86_400_000);
  }

  private hasScheduleConflict(
    requestedWeekdays: number[], requestedStart: string, requestedEnd: string,
    requestedCrossesMidnight: boolean, requestedStartDate: number, requestedEndDate: number,
    existingWeekdays: number[], existingStart: string, existingEnd: string,
    existingCrossesMidnight: boolean, existingStartDate: number, existingEndDate: number
  ): boolean {
    const monday = this.epochDay('2026-01-05');
    const requestedInterval = this.interval(requestedStart, requestedEnd, requestedCrossesMidnight);
    const existingInterval = this.interval(existingStart, existingEnd, existingCrossesMidnight);

    for (const requestedDay of requestedWeekdays) {
      for (let dayDifference = -1; dayDifference <= 1; dayDifference++) {
        const existingDay = ((requestedDay - 1 + dayDifference + 7) % 7) + 1;
        if (!existingWeekdays.includes(existingDay)) continue;
        if (!this.commonOccurrenceDate(
          requestedStartDate, requestedEndDate, existingStartDate, existingEndDate,
          requestedDay, dayDifference, monday
        )) continue;

        const shiftedExisting = {
          start: existingInterval.start + dayDifference * 1440,
          end: existingInterval.end + dayDifference * 1440
        };
        const requestedAbsolute = {
          start: requestedInterval.start,
          end: requestedInterval.end
        };
        // Igual que el backend: tocar exactamente a la misma hora no es conflicto.
        if (Math.max(requestedAbsolute.start, shiftedExisting.start)
          < Math.min(requestedAbsolute.end, shiftedExisting.end)) return true;
      }
    }
    return false;
  }

  private interval(start: string, end: string, crossesMidnight: boolean): { start: number; end: number } {
    const minutes = (value: string): number => {
      const [hour, minute] = value.substring(0, 5).split(':').map(Number);
      return hour * 60 + minute;
    };
    const startMinutes = minutes(start);
    let endMinutes = minutes(end);
    if (crossesMidnight) endMinutes += 1440;
    return { start: startMinutes, end: endMinutes };
  }

  private commonOccurrenceDate(
    requestedStart: number, requestedEnd: number, existingStart: number, existingEnd: number,
    requestedDay: number, dayDifference: number, monday: number
  ): boolean {
    const earliest = Math.max(requestedStart, existingStart - dayDifference);
    const latest = Math.min(requestedEnd, existingEnd === Infinity ? Infinity : existingEnd - dayDifference);
    if (earliest > latest) return false;
    const weekday = ((earliest - monday) % 7 + 7) % 7 + 1;
    const nextOccurrence = earliest + ((requestedDay - weekday + 7) % 7);
    return nextOccurrence <= latest;
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
