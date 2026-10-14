import {
  LeaveTypeResponse,
  LeaveBalanceResponse,
  LeaveRequestResponse,
  HolidayResponse,
  LeaveAttachmentResponse,
  LeaveHistoryResponse
} from '../../../../entities/absence';

export const MOCK_LEAVES: {
  leaveTypes: LeaveTypeResponse[];
  leaveBalances: LeaveBalanceResponse[];
  leaveRequests: LeaveRequestResponse[];
  holidays: HolidayResponse[];
  leaveAttachments: LeaveAttachmentResponse[];
  leaveHistory: LeaveHistoryResponse[];
} = {
  leaveTypes: [
    {
      id: '44444444-4444-4444-4444-444444444401',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: 'LEAVE_VACATION',
      name: 'Vacación Anual Legal (LGT Bolivia)',
      paid: true,
      isPaid: true,
      requiresApproval: true,
      requiresAttachment: false,
      deductsBalance: true,
      maxDaysPerYear: 30.0,
      description: 'Vacación obligatoria según escala de antigüedad LGT (15, 20 o 30 días hábiles).',
      status: 'ACTIVE' as const
    },
    {
      id: '44444444-4444-4444-4444-444444444402',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: 'LEAVE_MEDICAL',
      name: 'Baja Médica / Incapacidad Temporal',
      paid: true,
      isPaid: true,
      requiresApproval: true,
      requiresAttachment: true,
      deductsBalance: false,
      maxDaysPerYear: null,
      description: 'Baja avalada por la Caja de Salud o médico tratante. Requiere adjunto obligatorio de respaldo.',
      status: 'ACTIVE' as const
    },
    {
      id: '44444444-4444-4444-4444-444444444403',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: 'LEAVE_BEREAVEMENT',
      name: 'Licencia por Duelo / Luto',
      paid: true,
      isPaid: true,
      requiresApproval: true,
      requiresAttachment: true,
      deductsBalance: false,
      maxDaysPerYear: 3.0,
      description: 'Licencia legal por fallecimiento de familiar de primer grado (3 días hábiles).',
      status: 'ACTIVE' as const
    },
    {
      id: '44444444-4444-4444-4444-444444444404',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      code: 'LEAVE_PATERNITY',
      name: 'Licencia por Paternidad',
      paid: true,
      isPaid: true,
      requiresApproval: true,
      requiresAttachment: true,
      deductsBalance: false,
      maxDaysPerYear: 3.0,
      description: 'Licencia legal remunerada por nacimiento de hijo (3 días hábiles según Ley N° 3460).',
      status: 'ACTIVE' as const
    }
  ],
  leaveBalances: [
    {
      id: 'bal-001-44444444',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      leaveTypeId: '44444444-4444-4444-4444-444444444401',
      leaveTypeName: 'Vacación Anual Legal (LGT Bolivia)',
      balanceYear: 2026,
      year: 2026,
      hireDate: '2023-01-15',
      yearsOfService: 3,
      openingDays: 15.0,
      accruedDays: 0.0,
      adjustedDays: 0.0,
      entitledDays: 15.0,
      usedDays: 5.0,
      pendingDays: 3.0,
      availableDays: 7.0,
      remainingDays: 7.0,
      updatedAt: '2026-10-12T00:00:00Z'
    },
    {
      id: 'bal-002-44444444',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
      leaveTypeId: '44444444-4444-4444-4444-444444444401',
      leaveTypeName: 'Vacación Anual Legal (LGT Bolivia)',
      balanceYear: 2026,
      year: 2026,
      hireDate: '2018-06-01',
      yearsOfService: 8,
      openingDays: 20.0,
      accruedDays: 0.0,
      adjustedDays: 0.0,
      entitledDays: 20.0,
      usedDays: 10.0,
      pendingDays: 0.0,
      availableDays: 10.0,
      remainingDays: 10.0,
      updatedAt: '2026-10-12T00:00:00Z'
    }
  ],
  leaveRequests: [
    {
      id: 'req-001-44444444',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
      employeeFullName: 'Carlos Mamani Quispe',
      leaveTypeId: '44444444-4444-4444-4444-444444444401',
      leaveTypeName: 'Vacación Anual Legal (LGT Bolivia)',
      startDate: '2026-11-02',
      endDate: '2026-11-04',
      totalDays: 3.0,
      isHourly: false,
      startTime: null,
      endTime: null,
      totalHours: null,
      reason: 'Descanso programado por asuntos familiares personales.',
      attachmentUrl: null,
      status: 'PENDING' as const,
      reviewedBy: null,
      reviewedAt: null,
      rejectionReason: null,
      createdAt: '2026-10-10T14:30:00Z'
    },
    {
      id: 'req-002-44444444',
      tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c',
      employeeId: '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
      employeeFullName: 'Valeria Rios Salazar',
      leaveTypeId: '44444444-4444-4444-4444-444444444402',
      leaveTypeName: 'Baja Médica / Incapacidad Temporal',
      startDate: '2026-10-05',
      endDate: '2026-10-07',
      totalDays: 3.0,
      isHourly: false,
      startTime: null,
      endTime: null,
      totalHours: null,
      reason: 'Cuadro respiratorio agudo certificado por la Caja Nacional de Salud.',
      attachmentUrl: 'https://raw.githubusercontent.com/mozilla/pdf.js/ba2edeae/web/compressed.tracemonkey-pldi-09.pdf',
      status: 'APPROVED' as const,
      reviewedBy: '550e8400-e29b-41d4-a716-446655440000',
      reviewedAt: '2026-10-05T10:00:00Z',
      rejectionReason: null,
      createdAt: '2026-10-05T08:15:00Z'
    }
  ],
  holidays: [
    { id: 'hol-001', tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', name: 'Año Nuevo', holidayDate: '2026-01-01', isRecurring: true, scope: 'NATIONAL' as const },
    { id: 'hol-002', tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', name: 'Día del Estado Plurinacional', holidayDate: '2026-01-22', isRecurring: true, scope: 'NATIONAL' as const },
    { id: 'hol-003', tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', name: 'Día del Trabajo', holidayDate: '2026-05-01', isRecurring: true, scope: 'NATIONAL' as const },
    { id: 'hol-004', tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', name: 'Día de la Independencia', holidayDate: '2026-08-06', isRecurring: true, scope: 'NATIONAL' as const },
    { id: 'hol-005', tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', name: 'Todos Santos', holidayDate: '2026-11-02', isRecurring: true, scope: 'NATIONAL' as const },
    { id: 'hol-006', tenantId: 'e2a1b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', name: 'Navidad', holidayDate: '2026-12-25', isRecurring: true, scope: 'NATIONAL' as const }
  ],
  leaveAttachments: [
    {
      id: 'att-001-44444444',
      leaveRequestId: 'req-002-44444444',
      filename: 'certificado_medico_cns.pdf',
      mimeType: 'application/pdf',
      fileSize: 524288,
      uploadedBy: '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
      createdAt: '2026-10-05T08:15:00Z'
    }
  ],
  leaveHistory: [
    {
      id: 'his-001',
      leaveRequestId: 'req-002-44444444',
      previousStatus: 'DRAFT',
      newStatus: 'PENDING',
      comment: 'Solicitud enviada para revisión',
      performedBy: '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
      createdAt: '2026-10-05T08:16:00Z'
    },
    {
      id: 'his-002',
      leaveRequestId: 'req-002-44444444',
      previousStatus: 'PENDING',
      newStatus: 'APPROVED',
      comment: 'Aprobado según baja médica CNS',
      performedBy: '550e8400-e29b-41d4-a716-446655440000',
      createdAt: '2026-10-05T10:00:00Z'
    }
  ]
};
