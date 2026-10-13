import { ShiftDayResponse } from './shift-day.model';

export type ShiftType = 'CONTINUOUS' | 'SPLIT' | 'ROTATING' | 'NIGHT';

export interface ShiftResponse {
  id: string;                 // UUID
  tenantId: string;           // UUID
  code: string;               // VARCHAR(50)
  name: string;               // VARCHAR(100)
  shiftType: ShiftType;
  startTime: string;          // TIME (HH:mm:ss)
  endTime: string;            // TIME (HH:mm:ss)
  breakMinutes: number;       // INT
  gracePeriodMinutes: number; // Tolerancia (5 a 15 min, default 10)
  isOvernight: boolean;       // Cruce de medianoche (+1 día)
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
  days?: ShiftDayResponse[];
}

export interface CreateShiftRequest {
  code: string;
  name: string;
  shiftType: ShiftType;
  startTime: string;
  endTime: string;
  breakMinutes: number;
  gracePeriodMinutes: number;
  isOvernight: boolean;
  workDays: number[]; // Array de días [1, 2, 3, 4, 5]
}
