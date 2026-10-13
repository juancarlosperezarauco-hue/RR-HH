export type AttendanceStatus = 
  | 'PRESENT'
  | 'LATE'
  | 'EARLY_DEPARTURE'
  | 'ABSENT'
  | 'EXCUSED'
  | 'HOLIDAY'
  | 'REST_DAY';

export type PunchSource = 'WEB' | 'MOBILE' | 'BIOMETRIC' | 'MANUAL';

export interface AttendanceRecordResponse {
  id: string;               // UUID
  tenantId: string;         // UUID
  employeeId: string;       // UUID
  employeeFullName?: string;
  employeeCode?: string;
  shiftId?: string | null;  // UUID
  workDate: string;         // YYYY-MM-DD
  checkInTime?: string | null;  // ISO Timestamp server-side
  checkOutTime?: string | null; // ISO Timestamp server-side
  workedHours: number;      // Horas ordinarias computadas
  overtimeHours: number;    // Horas extraordinarias computadas
  lateMinutes: number;      // Retraso en minutos
  earlyDepartureMinutes: number;
  status: AttendanceStatus;
  source: PunchSource;
  latitude?: number | null;
  longitude?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ClockPunchRequest {
  employeeId: string;
  type: 'IN' | 'OUT' | 'BREAK_OUT' | 'BREAK_IN';
  method: PunchSource;
  latitude?: number;
  longitude?: number;
}
