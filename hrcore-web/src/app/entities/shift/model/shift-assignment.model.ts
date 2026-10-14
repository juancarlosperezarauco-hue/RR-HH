export type ShiftAssignmentStatus = 'ACTIVE' | 'INACTIVE' | 'CANCELLED' | 'CLOSED';

/**
 * DTO canónico de Spring Boot: ShiftAssignmentResponse
 * com.hrcoresystem.hrcore_api.modules.tenant.shifts.application.dto.ShiftAssignmentResponse
 * Endpoint: /api/shifts/assignments
 */
export interface ShiftAssignmentResponse {
  id: string;                      // UUID
  employeeId: string;              // UUID
  shiftId: string;                 // UUID
  startDate: string;               // LocalDate YYYY-MM-DD
  endDate?: string | null;         // LocalDate YYYY-MM-DD (null = indefinido)
  weekdays: number[];              // Array de días 1..7 (1=Lunes, 7=Domingo ISO-8601)
  status: ShiftAssignmentStatus;   // ACTIVE | INACTIVE | CANCELLED
  createdAt: string;               // ISO Instant
  updatedAt?: string;              // ISO Instant
}

/**
 * Tipo compatible con las vistas y tablas actuales
 */
export interface EmployeeShiftAssignmentResponse {
  id: string;                      // UUID
  employeeId: string;              // UUID
  shiftId: string;                 // UUID
  startDate: string;               // LocalDate YYYY-MM-DD
  endDate?: string | null;         // LocalDate YYYY-MM-DD (null = indefinido)
  weekdays?: number[];             // Array de días 1..7 (1=Lunes, 7=Domingo ISO-8601)
  status: ShiftAssignmentStatus | 'ACTIVE' | 'INACTIVE';   // ACTIVE | INACTIVE | CANCELLED
  createdAt: string;               // ISO Instant
  updatedAt?: string;              // ISO Instant

  tenantId?: string;
  employeeFullName?: string;
  employeeCode?: string;
  shiftName?: string;
  isRecurring?: boolean;
}

/**
 * DTO oficial de creación de asignación en Spring Boot: CreateShiftAssignmentRequest
 * POST /api/shifts/assignments
 */
export interface CreateShiftAssignmentRequest {
  employeeId: string;
  shiftId: string;
  startDate: string;
  endDate?: string | null;
  weekdays: number[]; // min 1, max 7
}

/**
 * DTO de actualización de asignación en Spring Boot: UpdateShiftAssignmentRequest
 * PUT /api/shifts/assignments/{id}
 */
export interface UpdateShiftAssignmentRequest {
  shiftId: string;
  startDate: string;
  endDate?: string | null;
  weekdays: number[];
}

/**
 * Request compatible con UI y modales existentes
 */
export interface AssignShiftRequest {
  employeeIds: string[];
  shiftId: string;
  startDate: string;
  endDate?: string | null;
  isRecurring?: boolean;
  weekdays?: number[];
}
