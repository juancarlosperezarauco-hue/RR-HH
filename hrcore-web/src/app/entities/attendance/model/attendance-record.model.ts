export type AttendanceStatus = 
  | 'PENDING'
  | 'PRESENT'
  | 'LATE'
  | 'ABSENT'
  | 'JUSTIFIED'
  | 'INCOMPLETE'
  | 'DAY_OFF'
  | 'EARLY_DEPARTURE'
  | 'EXCUSED'
  | 'HOLIDAY'
  | 'REST_DAY';

export type PunchSource = 'WEB' | 'MOBILE' | 'BIOMETRIC' | 'DEVICE' | 'MANUAL' | 'API';

export interface AttendanceRecordResponse {
  id: string;               // UUID
  employeeId: string;       // UUID
  shiftAssignmentId?: string | null; // UUID
  workDate: string;         // YYYY-MM-DD
  expectedStartAt?: string | null;   // ISO Instant
  expectedEndAt?: string | null;     // ISO Instant
  actualFirstInAt?: string | null;   // ISO Instant
  actualLastOutAt?: string | null;   // ISO Instant
  workedMinutes?: number;            // Minutos ordinarios calculados
  lateMinutes: number;               // Retraso en minutos
  earlyDepartureMinutes: number;     // Salida temprana en minutos
  status: AttendanceStatus;
  calculationDetails?: unknown;
  calculatedAt?: string | null;
  createdAt: string;
  updatedAt?: string;

  // Convenience & backward-compatibility aliases
  tenantId?: string;
  employeeFullName?: string;
  employeeCode?: string;
  shiftId?: string | null;
  checkInTime?: string | null;
  checkOutTime?: string | null;
  workedHours?: number;
  overtimeHours?: number;
  source?: PunchSource;
  latitude?: number | null;
  longitude?: number | null;
}

export interface CalculateRecordRequest {
  employeeId: string;
  workDate: string; // YYYY-MM-DD
}

export interface ClockPunchRequest {
  employeeId: string;
  type: 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END' | 'BREAK_OUT' | 'BREAK_IN';
  method?: PunchSource;
  source?: PunchSource;
  latitude?: number;
  longitude?: number;
  notes?: string;
}
