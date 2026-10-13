export interface EmployeeShiftAssignmentResponse {
  id: string;
  tenantId: string;
  employeeId: string;
  employeeFullName?: string;
  employeeCode?: string;
  shiftId: string;
  shiftName?: string;
  startDate: string;        // YYYY-MM-DD
  endDate?: string | null;  // YYYY-MM-DD (null = indefinido)
  isRecurring: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface AssignShiftRequest {
  employeeIds: string[]; // Asignación masiva o individual
  shiftId: string;
  startDate: string;
  endDate?: string | null;
  isRecurring: boolean;
}
