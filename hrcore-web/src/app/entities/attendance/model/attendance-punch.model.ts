import { PunchSource } from './attendance-record.model';

export type PunchType = 'IN' | 'OUT' | 'BREAK_START' | 'BREAK_END' | 'BREAK_OUT' | 'BREAK_IN';

export interface AttendancePunchResponse {
  id: string;                    // UUID
  employeeId: string;            // UUID
  attendanceRecordId?: string;   // UUID
  punchType: PunchType;
  punchedAt: string;             // ISO Instant server-side
  source: PunchSource;
  latitude?: number | null;
  longitude?: number | null;
  recordedBy?: string | null;
  notes?: string | null;
  createdAt: string;

  // Backwards-compatibility aliases
  tenantId?: string;
  punchTime?: string;
  deviceId?: string | null;
}

export interface CreatePunchInput {
  employeeId?: string;
  punchType: PunchType;
  punchedAt?: string;
  latitude?: number;
  longitude?: number;
  notes?: string;
}
