export interface AttendanceCorrectionResponse {
  id: string;
  tenantId?: string;
  attendanceRecordId: string;
  requestedBy: string;
  approvedBy?: string | null;
  originalCheckIn?: string | null;
  correctedCheckIn?: string | null;
  originalCheckOut?: string | null;
  correctedCheckOut?: string | null;
  reason: string;
  decisionNotes?: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  decidedAt?: string | null;
}

export interface RegularizeAttendanceRequest {
  attendanceRecordId: string;
  correctedCheckIn?: string;
  correctedCheckOut?: string;
  reason: string; // Mínimo 10 caracteres obligatorios
}
