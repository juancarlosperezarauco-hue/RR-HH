export interface LeaveBalanceResponse {
  id: string;                    // UUID
  employeeId: string;            // UUID
  leaveTypeId: string;           // UUID
  balanceYear?: number;          // Ej: 2026 (canonical backend)
  openingDays?: number;          // Días base/iniciales
  accruedDays?: number;          // Días devengados
  usedDays: number;              // Días ya gozados
  adjustedDays?: number;         // Ajustes manuales
  pendingDays: number;           // Días en solicitudes pendientes
  availableDays?: number;        // Saldo disponible (canonical backend)
  updatedAt?: string;

  // Backwards compatibility / UI view model aliases
  tenantId?: string;
  leaveTypeName?: string;
  year: number;                  // Requerido por componentes UI
  hireDate?: string;
  yearsOfService?: number;
  entitledDays: number;          // Requerido por componentes UI (LGT: 15, 20 o 30)
  remainingDays: number;         // Requerido por componentes UI
}

export interface CreateLeaveBalanceInput {
  employeeId: string;
  leaveTypeId: string;
  balanceYear: number;
  openingDays: number;
}

export interface AdjustLeaveBalanceInput {
  deltaDays: number;
  reason: string;
}
