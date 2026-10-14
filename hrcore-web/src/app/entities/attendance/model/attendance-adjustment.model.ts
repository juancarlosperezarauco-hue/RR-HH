export type AdjustmentType = 'MISSING_PUNCH' | 'TIME_CORRECTION' | 'STATUS_CORRECTION' | 'OTHER';
export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface AttendanceAdjustmentResponse {
  id: string;                    // UUID
  attendanceRecordId: string;    // UUID
  adjustmentType: AdjustmentType;
  reason: string;
  previousValues?: unknown;
  requestedValues: unknown;
  status: ReviewStatus;
  requestedBy: string;          // UUID
  reviewedBy?: string | null;   // UUID
  reviewedAt?: string | null;   // ISO Instant
  reviewComment?: string | null;
  appliedAt?: string | null;    // ISO Instant
  createdAt: string;
  updatedAt?: string;
}

export interface CreateAdjustmentRequest {
  attendanceRecordId: string;
  adjustmentType: AdjustmentType;
  reason: string;
  requestedValues: unknown;
}

export interface ReviewAdjustmentRequest {
  comment?: string;
}

export interface AttendanceOvertimeResponse {
  id: string;                    // UUID
  attendanceRecordId: string;    // UUID
  requestedMinutes: number;
  approvedMinutes?: number | null;
  reason?: string | null;
  status: ReviewStatus;
  approvedBy?: string | null;    // UUID
  approvedAt?: string | null;    // ISO Instant
  createdAt: string;
  updatedAt?: string;
}

export interface CreateOvertimeRequest {
  attendanceRecordId: string;
  requestedMinutes: number;
  reason?: string;
}

export interface ApproveOvertimeRequest {
  approvedMinutes: number;
  comment?: string;
}
