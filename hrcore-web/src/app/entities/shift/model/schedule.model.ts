/**
 * Determinación del horario efectivo de un empleado: ScheduleDetermination
 * com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ScheduleDetermination
 */
export type ScheduleDetermination = 'EXACT' | 'NO_WORK' | 'INDETERMINATE';

/**
 * Intervalo de jornada laboral resuelto para una fecha
 */
export interface ScheduleInterval {
  shiftId: string;    // UUID del turno aplicado
  start: string;      // LocalDateTime ISO (ej. 2026-10-14T08:00:00)
  end: string;        // LocalDateTime ISO (ej. 2026-10-14T16:00:00)
}

/**
 * DTO canónico de Horario Efectivo Resuelto: ScheduleResponse
 * Endpoint: GET /api/shifts/schedule?employeeId={uuid}&date={YYYY-MM-DD}
 */
export interface ScheduleResponse {
  employeeId: string;
  date: string;                       // LocalDate YYYY-MM-DD
  departmentId?: string | null;       // UUID del departamento en esa fecha
  calendarExceptionId?: string | null;// UUID de la excepción aplicada si corresponde
  source: string;                     // Origen de la regla aplicada
  determination: ScheduleDetermination; // EXACT, NO_WORK, INDETERMINATE
  reason?: string | null;             // Motivo (ej. "Feriado", "Turno regular")
  intervals: ScheduleInterval[];      // Intervalos de trabajo efectivos calculados
}
