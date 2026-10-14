/**
 * DTO canónico de Descansos de Turno: ShiftBreakResponse
 * com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftBreakResponse
 * Sub-recurso: /api/shifts/{id}/breaks
 */
export interface ShiftBreakResponse {
  id: string;              // UUID
  shiftId: string;         // UUID del turno padre
  name: string;            // VARCHAR(100), ej: "Almuerzo", "Refrigerio"
  startTime: string;       // TIME (HH:mm:ss)
  endTime: string;         // TIME (HH:mm:ss)
  durationMinutes: number; // INT (1 a 1440)
  paid: boolean;           // true si es remunerado, false si es descontado
  createdAt: string;       // ISO Instant
}

/**
 * DTO para crear o editar descanso en Spring Boot: ShiftBreakRequest
 * POST /api/shifts/{id}/breaks & PUT /api/shifts/{id}/breaks/{breakId}
 */
export interface ShiftBreakRequest {
  name: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  paid: boolean;
}
