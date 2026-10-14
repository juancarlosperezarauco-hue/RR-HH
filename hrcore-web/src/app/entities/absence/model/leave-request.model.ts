export type LeaveStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveRequestResponse {
  id: string;                    // UUID
  employeeId: string;            // UUID
  leaveTypeId: string;           // UUID
  startDate: string;             // YYYY-MM-DD
  endDate: string;               // YYYY-MM-DD
  startTime?: string | null;     // HH:mm:ss
  endTime?: string | null;       // HH:mm:ss
  requestedDays?: number;        // Días computados backend
  reason: string;
  status: LeaveStatus;
  requestedBy?: string | null;   // UUID
  submittedAt?: string | null;   // ISO Instant
  reviewedBy?: string | null;    // UUID
  reviewedAt?: string | null;    // ISO Instant
  reviewComment?: string | null; // Comentario de aprobación o rechazo
  createdAt: string;
  updatedAt?: string;

  // Backwards compatibility / UI view model aliases
  tenantId?: string;
  employeeFullName?: string;
  leaveTypeName?: string;
  totalDays: number;             // Requerido para plantillas y cálculos existentes
  isHourly?: boolean;
  totalHours?: number | null;
  attachmentUrl?: string | null;
  rejectionReason?: string | null; // Alias de reviewComment
}

export interface CreateLeaveRequestInput {
  employeeId: string;
  leaveTypeId: string;
  startDate: string;             // YYYY-MM-DD
  endDate: string;               // YYYY-MM-DD
  startTime?: string;            // HH:mm:ss
  endTime?: string;              // HH:mm:ss
  reason: string;
}

export interface CreateLeaveRequestPayload {
  employeeId: string;
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  reason: string;
  proofDocument?: File | null;
}

export interface ReviewLeaveInput {
  comment?: string;
}

export interface ReviewLeaveRequestPayload {
  requestId: string;
  approved: boolean;
  rejectionReason?: string;
}

export interface LeaveAttachmentResponse {
  id: string;                    // UUID
  leaveRequestId: string;        // UUID
  filename: string;
  mimeType: string;
  fileSize: number;
  uploadedBy?: string;
  createdAt: string;
}

export interface LeaveHistoryResponse {
  id: string;                    // UUID
  leaveRequestId: string;        // UUID
  previousStatus?: LeaveStatus;
  newStatus: LeaveStatus;
  comment?: string;
  performedBy?: string;
  createdAt: string;
}
