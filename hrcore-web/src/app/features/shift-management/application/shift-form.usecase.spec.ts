import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ShiftFormUseCase } from './shift-form.usecase';
import { ShiftService, WorkShiftRequest } from '../../../entities/shift';
import { ToastService } from '../../../shared/ui/toast/toast.service';

describe('ShiftFormUseCase (HU06 - Parametrización y Creación de Turnos LGT)', () => {
  let useCase: ShiftFormUseCase;
  let shiftServiceSpy: jasmine.SpyObj<ShiftService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  const validDiurnalShift: WorkShiftRequest = {
    name: 'Turno Administrativo Diurno',
    description: 'Jornada continua 8h',
    startTime: '08:30',
    endTime: '16:30',
    crossesMidnight: false,
    lateToleranceMinutes: 10,
    earlyDepartureToleranceMinutes: 5
  };

  const validNocturnalShift: WorkShiftRequest = {
    name: 'Turno Nocturno Regular',
    description: 'Jornada nocturna 7h',
    startTime: '22:00',
    endTime: '05:00',
    crossesMidnight: true,
    lateToleranceMinutes: 15,
    earlyDepartureToleranceMinutes: 10
  };

  beforeEach(() => {
    shiftServiceSpy = jasmine.createSpyObj('ShiftService', ['createShift', 'updateShift']);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning', 'info']);

    TestBed.configureTestingModule({
      providers: [
        ShiftFormUseCase,
        { provide: ShiftService, useValue: shiftServiceSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });

    useCase = TestBed.inject(ShiftFormUseCase);
  });

  describe('Validación de Jornada Laboral (LGT Bolivia)', () => {
    it('debe validar exitosamente un turno diurno conforme a ley (8 horas)', () => {
      const result = useCase.validateLaborLaw(validDiurnalShift);
      expect(result.isValid).toBeTrue();
      expect(result.durationHours).toBe(8);
      expect(result.isNocturnal).toBeFalse();
    });

    it('debe validar exitosamente un turno nocturno conforme a ley (7 horas)', () => {
      const result = useCase.validateLaborLaw(validNocturnalShift);
      expect(result.isValid).toBeTrue();
      expect(result.durationHours).toBe(7);
      expect(result.isNocturnal).toBeTrue();
    });

    it('debe rechazar un turno diurno que supere el límite legal de 8 horas diarias', () => {
      const excessiveShift: WorkShiftRequest = {
        ...validDiurnalShift,
        startTime: '08:00',
        endTime: '17:00' // 9 horas
      };
      const result = useCase.validateLaborLaw(excessiveShift);
      expect(result.isValid).toBeFalse();
      expect(result.message).toContain('supera el límite legal de 8 horas');
    });

    it('debe rechazar un turno nocturno que supere el límite legal de 7 horas diarias', () => {
      const excessiveNocturnal: WorkShiftRequest = {
        ...validNocturnalShift,
        startTime: '21:00',
        endTime: '05:00', // 8 horas
        crossesMidnight: true
      };
      const result = useCase.validateLaborLaw(excessiveNocturnal);
      expect(result.isValid).toBeFalse();
      expect(result.message).toContain('supera el límite legal de 7 horas');
    });

    it('debe rechazar tolerancias negativas', () => {
      const invalidTolerances: WorkShiftRequest = {
        ...validDiurnalShift,
        lateToleranceMinutes: -5
      };
      const result = useCase.validateLaborLaw(invalidTolerances);
      expect(result.isValid).toBeFalse();
      expect(result.message).toContain('Las tolerancias deben ser no negativas');
    });
  });

  describe('Creación y Guardado de Turnos (saveShift)', () => {
    it('debe crear un nuevo turno exitosamente llamando al servicio', async () => {
      shiftServiceSpy.createShift.and.returnValue(of({
        success: true,
        message: 'Turno creado correctamente',
        data: { id: 'shift-uuid-1', ...validDiurnalShift, code: 'TUR-001', status: 'ACTIVE', createdAt: '', updatedAt: '' },
        timestamp: ''
      }));

      const success = await useCase.saveShift(validDiurnalShift);

      expect(success).toBeTrue();
      expect(shiftServiceSpy.createShift).toHaveBeenCalledWith(validDiurnalShift);
      expect(toastSpy.success).toHaveBeenCalledWith(jasmine.stringContaining('exitosamente'));
      expect(useCase.validationError()).toBeNull();
    });

    it('debe actualizar un turno existente si se provee shiftId', async () => {
      shiftServiceSpy.updateShift.and.returnValue(of({
        success: true,
        message: 'Turno actualizado',
        data: { id: 'shift-uuid-1', ...validDiurnalShift, code: 'TUR-001', status: 'ACTIVE', createdAt: '', updatedAt: '' },
        timestamp: ''
      }));

      const success = await useCase.saveShift(validDiurnalShift, 'shift-uuid-1');

      expect(success).toBeTrue();
      expect(shiftServiceSpy.updateShift).toHaveBeenCalledWith('shift-uuid-1', validDiurnalShift);
      expect(toastSpy.success).toHaveBeenCalledWith(jasmine.stringContaining('actualizado'));
    });

    it('debe capturar errores de estado de negocio (422) extrayendo el detalle del error', async () => {
      const errorResponse = {
        status: 422,
        error: {
          success: false,
          message: 'La operación no está disponible en el estado actual',
          errors: ['Debe completar la configuración de la empresa antes de realizar esta operación']
        }
      };
      shiftServiceSpy.createShift.and.returnValue(throwError(() => errorResponse));

      const success = await useCase.saveShift(validDiurnalShift);

      expect(success).toBeFalse();
      expect(useCase.validationError()).toBe('Debe completar la configuración de la empresa antes de realizar esta operación');
      expect(toastSpy.error).toHaveBeenCalledWith('Debe completar la configuración de la empresa antes de realizar esta operación');
      expect(useCase.saving()).toBeFalse();
    });
  });
});
