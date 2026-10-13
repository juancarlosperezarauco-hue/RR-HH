export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveRequestResponse {
  id: string;               // UUID
  tenantId: string;         // UUID
  employeeId: string;       // UUID
  employeeFullName?: string;
  leaveTypeId: string;      // UUID
  leaveTypeName?: string;
  startDate: string;        // YYYY-MM-DD
  endDate: string;          // YYYY-MM-DD
  totalDays: number;        // Días hábiles computados
  isHourly: boolean;
  startTime?: string | null;
  endTime?: string | null;
  totalHours?: number | null;
  reason: string;
  attachmentUrl?: string | null; // Vinculado con archivo en HU15
  status: LeaveStatus;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  rejectionReason?: string | null;
  createdAt: string;
}

export interface CreateLeaveRequestPayload {
  employeeId: string;
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  reason: string;
  proofDocument?: File | null;
}

export interface ReviewLeaveRequestPayload {
  requestId: string;
  approved: boolean;
  rejectionReason?: string; // Obligatorio si approved = false (mín 10 caracteres)
}
