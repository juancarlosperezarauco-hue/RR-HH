export interface LeaveBalanceResponse {
  id: string;
  tenantId: string;
  employeeId: string;
  leaveTypeId: string;
  leaveTypeName?: string;
  year: number;
  hireDate?: string;
  yearsOfService?: number;
  entitledDays: number; // 15, 20 o 30 según LGT
  usedDays: number;
  pendingDays: number;
  remainingDays: number; // Saldo en tiempo real
  updatedAt: string;
}
