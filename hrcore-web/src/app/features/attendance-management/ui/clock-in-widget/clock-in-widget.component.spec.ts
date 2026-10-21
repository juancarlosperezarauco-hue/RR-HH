import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ClockInWidgetComponent } from './clock-in-widget.component';
import { AttendanceClockUseCase } from '../../application/attendance-clock.usecase';
import { AttendanceService } from '../../../../entities/attendance';
import { EmployeesService, EmployeeSummary } from '../../../../entities/employees';
import { ToastService } from '../../../../shared/ui/toast/toast.service';

describe('ClockInWidgetComponent (HU17 - Marcación de Asistencia & Máquina de Estados)', () => {
  let component: ClockInWidgetComponent;
  let fixture: ComponentFixture<ClockInWidgetComponent>;
  let employeesServiceSpy: jasmine.SpyObj<EmployeesService>;
  let attendanceServiceSpy: jasmine.SpyObj<AttendanceService>;
  let toastSpy: jasmine.SpyObj<ToastService>;
  let clockUseCaseSpy: jasmine.SpyObj<AttendanceClockUseCase>;

  const mockEmployees: EmployeeSummary[] = [
    {
      id: 'emp-001',
      employeeNumber: 'EMP-001',
      firstName: 'Carlos',
      lastName: 'Mendoza',
      employmentStatus: 'ACTIVE'
    },
    {
      id: 'emp-002',
      employeeNumber: 'EMP-002',
      firstName: 'Laura',
      lastName: 'Gómez',
      employmentStatus: 'ACTIVE'
    }
  ];

  beforeEach(async () => {
    employeesServiceSpy = jasmine.createSpyObj('EmployeesService', ['list']);
    attendanceServiceSpy = jasmine.createSpyObj('AttendanceService', ['listPunches', 'registerClock']);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning', 'info']);
    clockUseCaseSpy = jasmine.createSpyObj('AttendanceClockUseCase', [
      'registerClock',
      'fetchTodayRecord',
      'checkDebounceTimer'
    ], {
      clocking: jasmine.createSpy('clocking').and.returnValue(false),
      isDebounced: jasmine.createSpy('isDebounced').and.returnValue(false),
      remainingDebounceSeconds: jasmine.createSpy('remainingDebounceSeconds').and.returnValue(0),
      gpsAvailable: jasmine.createSpy('gpsAvailable').and.returnValue(true)
    });

    employeesServiceSpy.list.and.returnValue(of({
      success: true,
      message: 'OK',
      data: mockEmployees as any,
      timestamp: ''
    }));

    attendanceServiceSpy.listPunches.and.returnValue(of({
      success: true,
      message: 'OK',
      data: [] as any,
      timestamp: ''
    }));

    clockUseCaseSpy.fetchTodayRecord.and.returnValue(Promise.resolve(null));

    await TestBed.configureTestingModule({
      imports: [ClockInWidgetComponent],
      providers: [
        { provide: EmployeesService, useValue: employeesServiceSpy },
        { provide: AttendanceService, useValue: attendanceServiceSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: AttendanceClockUseCase, useValue: clockUseCaseSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClockInWidgetComponent);
    component = fixture.componentInstance;
  });

  describe('Carga y Población de Colaboradores', () => {
    it('debe mapear correctamente la lista de colaboradores cuando response.data es un array directo', async () => {
      await component.loadEmployees();

      expect(component.employeeOptions.length).toBe(2);
      expect(component.employeeOptions[0].name).toBe('Carlos Mendoza');
      expect(component.employeeOptions[0].code).toBe('EMP-001');
      expect(component.selectedEmployeeId).toBe('emp-001');
    });

    it('debe soportar la carga cuando response.data viene paginado con content', async () => {
      employeesServiceSpy.list.and.returnValue(of({
        success: true,
        message: 'OK',
        data: { content: mockEmployees } as any,
        timestamp: ''
      }));

      await component.loadEmployees();

      expect(component.employeeOptions.length).toBe(2);
      expect(component.employeeOptions[1].name).toBe('Laura Gómez');
    });

    it('debe actualizar employeeOptions inmediatamente cuando se provee el @Input() employees', () => {
      component.employees = mockEmployees;
      component.ngOnChanges({
        employees: {
          currentValue: mockEmployees,
          previousValue: [],
          firstChange: true,
          isFirstChange: () => true
        }
      });

      expect(component.employeeOptions.length).toBe(2);
      expect(component.selectedEmployeeId).toBe('emp-001');
    });
  });

  describe('Validación de la Máquina de Estados de Marcación (IN -> BREAK -> OUT)', () => {
    it('Estado Inicial (NONE): debe habilitar sólo Entrada y deshabilitar Pausa, Retorno y Salida', () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('NONE');

      expect(component.canPunchIn()).toBeTrue();
      expect(component.canPunchBreakStart()).toBeFalse();
      expect(component.canPunchBreakEnd()).toBeFalse();
      expect(component.canPunchOut()).toBeFalse();
      expect(component.statusLabel()).toContain('Esperando Entrada');
    });

    it('Estado En Jornada (IN): debe habilitar Pausa y Salida, y deshabilitar Entrada y Retorno', () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('IN');
      component.lastPunchTime.set('08:30');

      expect(component.canPunchIn()).toBeFalse();
      expect(component.canPunchBreakStart()).toBeTrue();
      expect(component.canPunchBreakEnd()).toBeFalse();
      expect(component.canPunchOut()).toBeTrue();
      expect(component.statusLabel()).toContain('En jornada laboral');
    });

    it('Estado En Pausa (BREAK_START): debe habilitar sólo Retorno y deshabilitar Salida y Pausa', () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('BREAK_START');
      component.lastPunchTime.set('12:30');

      expect(component.canPunchIn()).toBeFalse();
      expect(component.canPunchBreakStart()).toBeFalse();
      expect(component.canPunchBreakEnd()).toBeTrue();
      expect(component.canPunchOut()).toBeFalse();
      expect(component.statusLabel()).toContain('En descanso / pausa');
    });

    it('Estado Retorno de Pausa (BREAK_END): debe volver a habilitar Pausa y Salida', () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('BREAK_END');
      component.lastPunchTime.set('13:30');

      expect(component.canPunchIn()).toBeFalse();
      expect(component.canPunchBreakStart()).toBeTrue();
      expect(component.canPunchBreakEnd()).toBeFalse();
      expect(component.canPunchOut()).toBeTrue();
      expect(component.statusLabel()).toContain('Retorno de pausa');
    });

    it('Estado Salida (OUT): debe permitir Entrada para nueva jornada y bloquear Pausa, Retorno y Salida', () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('OUT');
      component.lastPunchTime.set('16:30');

      expect(component.canPunchIn()).toBeTrue();
      expect(component.canPunchBreakStart()).toBeFalse();
      expect(component.canPunchBreakEnd()).toBeFalse();
      expect(component.canPunchOut()).toBeFalse();
      expect(component.statusLabel()).toContain('Jornada concluida');
    });
  });

  describe('Prevención y Manejo de Errores en Marcación (punch)', () => {
    it('debe alertar preventivamente si el usuario intenta marcar Salida sin haber marcado Entrada', async () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('NONE');

      await component.punch('OUT');

      expect(toastSpy.warning).toHaveBeenCalledWith(jasmine.stringContaining('Debe registrar la entrada antes'));
      expect(clockUseCaseSpy.registerClock).not.toHaveBeenCalled();
    });

    it('debe alertar preventivamente si el usuario intenta marcar Salida estando en Pausa', async () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('BREAK_START');

      await component.punch('OUT');

      expect(toastSpy.warning).toHaveBeenCalledWith(jasmine.stringContaining('Debe registrar el retorno antes de marcar salida'));
      expect(clockUseCaseSpy.registerClock).not.toHaveBeenCalled();
    });

    it('debe alertar preventivamente si el usuario intenta marcar Retorno sin haber iniciado Pausa', async () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('IN');

      await component.punch('BREAK_END');

      expect(toastSpy.warning).toHaveBeenCalledWith(jasmine.stringContaining('Debe iniciar una pausa antes'));
      expect(clockUseCaseSpy.registerClock).not.toHaveBeenCalled();
    });

    it('debe procesar la marcación válida y actualizar el estado tras el registro exitoso', async () => {
      component.selectedEmployeeId = 'emp-001';
      component.lastPunchState.set('NONE');

      const mockRecordResponse: any = {
        id: 'rec-001',
        employeeId: 'emp-001',
        workDate: '2026-10-19',
        checkInTime: '2026-10-19T08:30:00Z',
        status: 'PRESENT'
      };

      clockUseCaseSpy.registerClock.and.returnValue(Promise.resolve(mockRecordResponse));
      spyOn(component.clockRegistered, 'emit');

      await component.punch('IN');

      expect(clockUseCaseSpy.registerClock).toHaveBeenCalledWith('emp-001', 'IN');
      expect(component.clockRegistered.emit).toHaveBeenCalledWith(mockRecordResponse);
    });
  });
});
