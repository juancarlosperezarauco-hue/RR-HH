/**
 * Tipos de excepción de calendario laboral: CalendarExceptionType
 * com.hrcoresystem.hrcore_api.modules.tenant.shifts.domain.model.CalendarExceptionType
 */
export type CalendarExceptionType =
  | 'HOLIDAY'
  | 'NON_WORKING_DAY'
  | 'SPECIAL_WORKING_DAY'
  | 'SCHEDULE_CHANGE'
  | 'OTHER';

export const CALENDAR_EXCEPTION_LABELS: Record<CalendarExceptionType, string> = {
  HOLIDAY: 'Feriado Nacional / Regional',
  NON_WORKING_DAY: 'Día No Laborable',
  SPECIAL_WORKING_DAY: 'Jornada Especial de Trabajo',
  SCHEDULE_CHANGE: 'Cambio Temporal de Horario',
  OTHER: 'Otra Excepción'
};

/**
 * DTO canónico de Excepción de Calendario: WorkCalendarExceptionResponse
 * Sub-recurso: /api/shifts/calendar-exceptions
 */
export interface WorkCalendarExceptionResponse {
  id: string;                               // UUID
  employeeId?: string | null;               // UUID opcional (excepción por empleado)
  departmentId?: string | null;             // UUID opcional (excepción por departamento)
  exceptionDate: string;                    // LocalDate YYYY-MM-DD
  exceptionType: CalendarExceptionType;
  description?: string | null;
  workRequired: boolean;                    // true si se debe trabajar, false si es asueto/feriado
  createdAt: string;                        // ISO Instant
}

/**
 * Request para crear o actualizar excepción de calendario: WorkCalendarExceptionRequest
 * POST & PUT /api/shifts/calendar-exceptions
 */
export interface WorkCalendarExceptionRequest {
  employeeId?: string | null;
  departmentId?: string | null;
  exceptionDate: string;
  exceptionType: CalendarExceptionType;
  description?: string;
  workRequired: boolean;
}
