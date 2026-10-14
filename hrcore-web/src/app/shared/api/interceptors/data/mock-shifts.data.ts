import { ShiftResponse, EmployeeShiftAssignmentResponse } from '../../../../entities/shift';

export const MOCK_SHIFTS: {
  shifts: ShiftResponse[];
  employeeShiftAssignments: EmployeeShiftAssignmentResponse[];
} = {
  shifts: [
    {
      id: '22222222-2222-2222-2222-222222222201',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: 'TUR_DIURNO_OFI',
      name: 'Turno Oficina Central (LGT 8 Horas)',
      description: 'Jornada continua diurna administrativa',
      shiftType: 'CONTINUOUS' as const,
      startTime: '08:30:00',
      endTime: '16:30:00',
      crossesMidnight: false,
      lateToleranceMinutes: 10,
      earlyDepartureToleranceMinutes: 5,
      breakMinutes: 45,
      gracePeriodMinutes: 10,
      isOvernight: false,
      status: 'ACTIVE' as const,
      createdAt: '2026-01-10T08:00:00Z',
      updatedAt: '2026-01-10T08:00:00Z',
      days: [
        { dayOfWeek: 1, isWorkDay: true, startTime: '08:30:00', endTime: '16:30:00', breakMinutes: 45 },
        { dayOfWeek: 2, isWorkDay: true, startTime: '08:30:00', endTime: '16:30:00', breakMinutes: 45 },
        { dayOfWeek: 3, isWorkDay: true, startTime: '08:30:00', endTime: '16:30:00', breakMinutes: 45 },
        { dayOfWeek: 4, isWorkDay: true, startTime: '08:30:00', endTime: '16:30:00', breakMinutes: 45 },
        { dayOfWeek: 5, isWorkDay: true, startTime: '08:30:00', endTime: '16:30:00', breakMinutes: 45 },
        { dayOfWeek: 6, isWorkDay: false, startTime: null, endTime: null, breakMinutes: 0 },
        { dayOfWeek: 7, isWorkDay: false, startTime: null, endTime: null, breakMinutes: 0 }
      ]
    },
    {
      id: '22222222-2222-2222-2222-222222222202',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: 'TUR_NOCT_SEG',
      name: 'Turno Nocturno Seguridad (LGT 7 Horas Máx)',
      description: 'Vigilancia nocturna con cruce de medianoche',
      shiftType: 'NIGHT' as const,
      startTime: '22:00:00',
      endTime: '05:00:00',
      crossesMidnight: true,
      lateToleranceMinutes: 15,
      earlyDepartureToleranceMinutes: 10,
      breakMinutes: 30,
      gracePeriodMinutes: 15,
      isOvernight: true,
      status: 'ACTIVE' as const,
      createdAt: '2026-01-10T08:00:00Z',
      updatedAt: '2026-01-10T08:00:00Z',
      days: [
        { dayOfWeek: 1, isWorkDay: true, startTime: '22:00:00', endTime: '05:00:00', breakMinutes: 30 },
        { dayOfWeek: 2, isWorkDay: true, startTime: '22:00:00', endTime: '05:00:00', breakMinutes: 30 },
        { dayOfWeek: 3, isWorkDay: true, startTime: '22:00:00', endTime: '05:00:00', breakMinutes: 30 },
        { dayOfWeek: 4, isWorkDay: true, startTime: '22:00:00', endTime: '05:00:00', breakMinutes: 30 },
        { dayOfWeek: 5, isWorkDay: true, startTime: '22:00:00', endTime: '05:00:00', breakMinutes: 30 },
        { dayOfWeek: 6, isWorkDay: true, startTime: '22:00:00', endTime: '05:00:00', breakMinutes: 30 },
        { dayOfWeek: 7, isWorkDay: false, startTime: null, endTime: null, breakMinutes: 0 }
      ]
    },
    {
      id: '22222222-2222-2222-2222-222222222203',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: 'TUR_ROTAT_PLANTA',
      name: 'Turno Rotativo Planta Industrial',
      description: 'Turno rotativo diurno matutino',
      shiftType: 'ROTATING' as const,
      startTime: '06:00:00',
      endTime: '14:00:00',
      crossesMidnight: false,
      lateToleranceMinutes: 10,
      earlyDepartureToleranceMinutes: 5,
      breakMinutes: 30,
      gracePeriodMinutes: 10,
      isOvernight: false,
      status: 'ACTIVE' as const,
      createdAt: '2026-01-15T10:00:00Z',
      updatedAt: '2026-01-15T10:00:00Z',
      days: [
        { dayOfWeek: 1, isWorkDay: true, startTime: '06:00:00', endTime: '14:00:00', breakMinutes: 30 },
        { dayOfWeek: 2, isWorkDay: true, startTime: '06:00:00', endTime: '14:00:00', breakMinutes: 30 },
        { dayOfWeek: 3, isWorkDay: true, startTime: '06:00:00', endTime: '14:00:00', breakMinutes: 30 },
        { dayOfWeek: 4, isWorkDay: true, startTime: '06:00:00', endTime: '14:00:00', breakMinutes: 30 },
        { dayOfWeek: 5, isWorkDay: true, startTime: '06:00:00', endTime: '14:00:00', breakMinutes: 30 },
        { dayOfWeek: 6, isWorkDay: true, startTime: '06:00:00', endTime: '12:00:00', breakMinutes: 15 },
        { dayOfWeek: 7, isWorkDay: false, startTime: null, endTime: null, breakMinutes: 0 }
      ]
    }
  ],
  employeeShiftAssignments: [
    {
      id: 'asg-0001-22222222',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      employeeFullName: 'Carlos Mamani Quispe',
      employeeCode: 'EMP-00104',
      shiftId: '22222222-2222-2222-2222-222222222201',
      shiftName: 'Turno Oficina Central (LGT 8 Horas)',
      startDate: '2026-02-01',
      endDate: null,
      weekdays: [1, 2, 3, 4, 5],
      isRecurring: true,
      status: 'ACTIVE' as const,
      createdAt: '2026-02-01T08:00:00Z'
    },
    {
      id: 'asg-0002-22222222',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
      employeeFullName: 'Valeria Rios Salazar',
      employeeCode: 'EMP-00105',
      shiftId: '22222222-2222-2222-2222-222222222202',
      shiftName: 'Turno Nocturno Seguridad (LGT 7 Horas Máx)',
      startDate: '2026-02-01',
      endDate: '2026-06-30',
      weekdays: [1, 2, 3, 4, 5, 6],
      isRecurring: true,
      status: 'ACTIVE' as const,
      createdAt: '2026-02-01T08:00:00Z'
    }
  ]
};
