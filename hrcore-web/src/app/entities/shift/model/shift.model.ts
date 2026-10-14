import { ShiftDayResponse } from './shift-day.model';

export type ShiftStatus = 'ACTIVE' | 'INACTIVE';
export type ShiftType = 'CONTINUOUS' | 'SPLIT' | 'ROTATING' | 'NIGHT';

/**
 * DTO canónico estricto de Spring Boot: WorkShiftResponse
 * com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.WorkShiftResponse
 */
export interface WorkShiftResponse {
  id: string;                               // UUID
  code: string;                             // VARCHAR(50)
  name: string;                             // VARCHAR(150)
  description?: string | null;              // Texto libre
  startTime: string;                        // TIME (HH:mm:ss)
  endTime: string;                          // TIME (HH:mm:ss)
  crossesMidnight: boolean;                 // Cruce de medianoche (+1 día)
  lateToleranceMinutes: number;             // Tolerancia llegada tarde (minutos)
  earlyDepartureToleranceMinutes: number;   // Tolerancia salida temprana (minutos)
  status: ShiftStatus;                      // ACTIVE | INACTIVE
  createdAt: string;                        // ISO Instant
  updatedAt: string;                        // ISO Instant
}

/**
 * Tipo enriquecido para compatibilidad con vistas y componentes UI actuales
 */
export interface ShiftResponse {
  id: string;                               // UUID
  code: string;                             // VARCHAR(50)
  name: string;                             // VARCHAR(150)
  description?: string | null;              // Texto libre
  startTime: string;                        // TIME (HH:mm:ss)
  endTime: string;                          // TIME (HH:mm:ss)
  status: ShiftStatus;                      // ACTIVE | INACTIVE
  createdAt: string;                        // ISO Instant
  updatedAt: string;                        // ISO Instant

  // Campos de Spring Boot
  crossesMidnight?: boolean;
  lateToleranceMinutes?: number;
  earlyDepartureToleranceMinutes?: number;

  // Campos requeridos por UI actual y Mocks
  tenantId?: string;
  shiftType: ShiftType;
  breakMinutes: number;
  gracePeriodMinutes: number;
  isOvernight: boolean;
  days?: ShiftDayResponse[];
}

/**
 * DTO canónico de creación/edición de turno en Spring Boot: WorkShiftRequest
 * POST /api/shifts & PUT /api/shifts/{id}
 */
export interface WorkShiftRequest {
  code: string;
  name: string;
  description?: string;
  startTime: string;
  endTime: string;
  crossesMidnight: boolean;
  lateToleranceMinutes: number;
  earlyDepartureToleranceMinutes: number;
}

/**
 * Request compatible con los formularios y modales actuales de UI
 */
export interface CreateShiftRequest {
  code: string;
  name: string;
  description?: string;
  startTime: string;
  endTime: string;

  // Campos de Spring Boot opcionales durante la transición
  crossesMidnight?: boolean;
  lateToleranceMinutes?: number;
  earlyDepartureToleranceMinutes?: number;

  // Campos de formulario UI requeridos
  shiftType: ShiftType;
  breakMinutes: number;
  gracePeriodMinutes: number;
  isOvernight: boolean;
  workDays: number[]; // [1, 2, 3, 4, 5]
}
