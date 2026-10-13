import { PunchSource } from './attendance-record.model';

export type PunchType = 'IN' | 'OUT' | 'BREAK_OUT' | 'BREAK_IN';

export interface AttendancePunchResponse {
  id: string;                    // UUID
  attendanceRecordId: string;    // UUID
  tenantId: string;              // UUID
  employeeId: string;            // UUID
  punchTime: string;             // ISO Timestamp server-side
  punchType: PunchType;
  source: PunchSource;
  deviceId?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  createdAt: string;
}
