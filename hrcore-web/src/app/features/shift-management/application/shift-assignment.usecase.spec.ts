import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ShiftAssignmentUseCase } from './shift-assignment.usecase';
import { ShiftService, AssignShiftRequest, EmployeeShiftAssignmentResponse } from '../../../entities/shift';
import { EmployeesService } from '../../../entities/employees';
import { ToastService } from '../../../shared/ui/toast/toast.service';

describe('ShiftAssignmentUseCase (HU07 - Asignación de Turnos a Empleados)', () => {
  let useCase: ShiftAssignmentUseCase;
  let shiftServiceSpy: jasmine.SpyObj<ShiftService>;
  let employeesServiceSpy: jasmine.SpyObj<EmployeesService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  const mockActiveAssignment: EmployeeShiftAssignmentResponse = {
    id: 'asg-001',
    employeeId: 'emp-001',
    shiftId: 'shift-001',
    employmentPeriodId: 'per-001',
    startDate: '2026-10-01',
    endDate: '2026-10-31',
    weekdays: [1, 2, 3, 4, 5],
    status: 'ACTIVE',
    shiftName: 'Turno Administrativo',
    employeeFullName: 'Juan Perez'
  };

  const validAssignRequest: AssignShiftRequest = {
    shiftId: 'shift-002',
    employeeIds: ['emp-002'],
    startDate: '2026-11-01',
    endDate: '2026-11-30',
    weekdays: [1, 2, 3, 4, 5]
  };

  beforeEach(() => {
    shiftServiceSpy = jasmine.createSpyObj('ShiftService', [
      'listAssignments',
      'assignShifts',
      'cancelAssignment',
      'closeAssignment',
      'listShifts'
    ]);
    employeesServiceSpy = jasmine.createSpyObj('EmployeesService', ['list']);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning', 'info']);

    TestBed.configureTestingModule({
      providers: [
        ShiftAssignmentUseCase,
        { provide: ShiftService, useValue: shiftServiceSpy },
        { provide: EmployeesService, useValue: employeesServiceSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });

    useCase = TestBed.inject(ShiftAssignmentUseCase);
  });

  describe('Detección y Prevención de Solapamientos (validateOverlap)', () => {
    it('debe detectar solapamiento si el empleado ya tiene un turno activo en el rango indicado', () => {
      useCase.assignments.set([mockActiveAssignment]);

      // Intentar asignar del 15 al 25 de octubre (dentro de 01-10 a 31-10)
      const overlap = useCase.validateOverlap('emp-001', '2026-10-15', '2026-10-25');

      expect(overlap.hasOverlap).toBeTrue();
      expect(overlap.message).toContain('ya tiene el turno "Turno Administrativo" asignado');
    });

    it('debe permitir asignar si las fechas no se solapan con los turnos activos existentes', () => {
      useCase.assignments.set([mockActiveAssignment]);

      // Asignar en noviembre (fuera de octubre)
      const overlap = useCase.validateOverlap('emp-001', '2026-11-01', '2026-11-30');

      expect(overlap.hasOverlap).toBeFalse();
    });

    it('debe ignorar turnos cancelados o inactivos al validar solapamiento', () => {
      useCase.assignments.set([{
        ...mockActiveAssignment,
        status: 'CANCELLED'
      }]);

      const overlap = useCase.validateOverlap('emp-001', '2026-10-15', '2026-10-25');
      expect(overlap.hasOverlap).toBeFalse();
    });
  });

  describe('Asignación de Turnos (assign)', () => {
    it('debe asignar el turno exitosamente a los colaboradores seleccionados', async () => {
      useCase.assignments.set([]);
      const newAssignment: EmployeeShiftAssignmentResponse = {
        id: 'asg-002',
        employeeId: 'emp-002',
        shiftId: 'shift-002',
        startDate: '2026-11-01',
        endDate: '2026-11-30',
        weekdays: [1, 2, 3, 4, 5],
        status: 'ACTIVE'
      };

      shiftServiceSpy.assignShifts.and.returnValue(of({
        success: true,
        message: 'Turnos asignados correctamente',
        data: [newAssignment],
        timestamp: ''
      }));

      const success = await useCase.assign(validAssignRequest);

      expect(success).toBeTrue();
      expect(shiftServiceSpy.assignShifts).toHaveBeenCalledWith({
        ...validAssignRequest,
        weekdays: [1, 2, 3, 4, 5]
      });
      expect(toastSpy.success).toHaveBeenCalledWith(jasmine.stringContaining('Turno asignado a 1 colaborador(es)'));
      expect(useCase.assignments()).toContain(jasmine.objectContaining({ id: 'asg-002' }));
    });

    it('debe rechazar la asignación si se detecta solapamiento en alguno de los empleados', async () => {
      useCase.assignments.set([mockActiveAssignment]);

      const conflictingRequest: AssignShiftRequest = {
        shiftId: 'shift-999',
        employeeIds: ['emp-001'],
        startDate: '2026-10-20',
        endDate: '2026-10-25'
      };

      const success = await useCase.assign(conflictingRequest);

      expect(success).toBeFalse();
      expect(shiftServiceSpy.assignShifts).not.toHaveBeenCalled();
      expect(toastSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('ya tiene el turno'));
    });

    it('debe extraer el mensaje detallado de error (errors[0]) si el backend retorna 422', async () => {
      useCase.assignments.set([]);
      const errorResponse = {
        status: 422,
        error: {
          success: false,
          message: 'La operación no está disponible en el estado actual',
          errors: ['Debe completar la configuración de la empresa antes de realizar esta operación']
        }
      };
      shiftServiceSpy.assignShifts.and.returnValue(throwError(() => errorResponse));

      const success = await useCase.assign(validAssignRequest);

      expect(success).toBeFalse();
      expect(useCase.error()).toBe('Debe completar la configuración de la empresa antes de realizar esta operación');
      expect(toastSpy.error).toHaveBeenCalledWith('Debe completar la configuración de la empresa antes de realizar esta operación');
    });
  });

  describe('Gestión de Estado de Asignaciones (cancel y close)', () => {
    it('debe cancelar una asignación activa y actualizar su estado en la lista', async () => {
      useCase.assignments.set([mockActiveAssignment]);
      shiftServiceSpy.cancelAssignment.and.returnValue(of({
        success: true,
        message: 'Asignación cancelada',
        data: { ...mockActiveAssignment, status: 'CANCELLED' },
        timestamp: ''
      }));

      const success = await useCase.cancelAssignment('asg-001');

      expect(success).toBeTrue();
      expect(shiftServiceSpy.cancelAssignment).toHaveBeenCalledWith('asg-001');
      expect(useCase.assignments()[0].status).toBe('CANCELLED');
    });

    it('debe cerrar una asignación y actualizar su estado a INACTIVE', async () => {
      useCase.assignments.set([mockActiveAssignment]);
      shiftServiceSpy.closeAssignment.and.returnValue(of({
        success: true,
        message: 'Asignación cerrada',
        data: { ...mockActiveAssignment, status: 'INACTIVE' },
        timestamp: ''
      }));

      const success = await useCase.closeAssignment('asg-001');

      expect(success).toBeTrue();
      expect(shiftServiceSpy.closeAssignment).toHaveBeenCalledWith('asg-001');
      expect(useCase.assignments()[0].status).toBe('INACTIVE');
    });
  });
});
