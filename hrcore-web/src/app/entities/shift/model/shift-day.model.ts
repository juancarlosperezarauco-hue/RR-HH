export interface ShiftDayResponse {
  id?: string;
  shiftId?: string;
  dayOfWeek: number; // 1: Lunes, 7: Domingo
  isWorkDay: boolean;
  startTime?: string | null; // HH:mm:ss
  endTime?: string | null;   // HH:mm:ss
  breakMinutes: number;
}
