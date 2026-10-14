import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import {
  ShiftResponse,
  WorkShiftResponse,
  WorkShiftRequest,
  CreateShiftRequest,
  ShiftStatus
} from '../model/shift.model';
import {
  ShiftBreakResponse,
  ShiftBreakRequest
} from '../model/shift-break.model';
import {
  ShiftAssignmentResponse,
  EmployeeShiftAssignmentResponse,
  CreateShiftAssignmentRequest,
  UpdateShiftAssignmentRequest,
  AssignShiftRequest,
  ShiftAssignmentStatus
} from '../model/shift-assignment.model';
import {
  WorkCalendarExceptionResponse,
  WorkCalendarExceptionRequest,
  CalendarExceptionType
} from '../model/calendar-exception.model';
import { ScheduleResponse } from '../model/schedule.model';

@Injectable({ providedIn: 'root' })
export class ShiftService {
  private readonly http = inject(HttpClient);
  // URL base canónica de Spring Boot: /api/shifts
  private readonly baseUrl = `${environment.apiUrl}/api/shifts`;
  // URL legada para interceptor mock /api/v1/shifts
  private readonly legacyUrl = `${environment.apiUrl}/api/v1/shifts`;

  // =========================================================================
  // 1. GESTIÓN DE TURNOS (WorkShift - /api/shifts)
  // =========================================================================

  /**
   * Listar y filtrar turnos
   * GET /api/shifts
   */
  listShifts(
    status?: 'ACTIVE' | 'INACTIVE' | 'ALL',
    page = 0,
    size = 50,
    code?: string,
    name?: string,
    sort = 'name',
    direction = 'asc'
  ): Observable<ApiResponse<PageResponse<ShiftResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', Math.min(Math.max(1, size), 100).toString())
      .set('sort', sort)
      .set('direction', direction);

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (code) params = params.set('code', code);
    if (name) params = params.set('name', name);

    return this.http.get<ApiResponse<PageResponse<ShiftResponse>>>(this.baseUrl, { params });
  }

  /**
   * Consultar detalle de un turno por ID
   * GET /api/shifts/{id}
   */
  getShiftById(id: string): Observable<ApiResponse<WorkShiftResponse>> {
    return this.http.get<ApiResponse<WorkShiftResponse>>(`${this.baseUrl}/${id}`);
  }

  /**
   * Crear nuevo turno
   * POST /api/shifts
   */
  createShift(request: WorkShiftRequest | CreateShiftRequest): Observable<ApiResponse<ShiftResponse>> {
    // Normalizar payload enviando campos oficiales de Spring Boot
    const payload: WorkShiftRequest = {
      code: request.code,
      name: request.name,
      description: request.description,
      startTime: request.startTime,
      endTime: request.endTime,
      crossesMidnight: request.crossesMidnight ?? (request as CreateShiftRequest).isOvernight ?? false,
      lateToleranceMinutes: request.lateToleranceMinutes ?? (request as CreateShiftRequest).gracePeriodMinutes ?? 10,
      earlyDepartureToleranceMinutes: request.earlyDepartureToleranceMinutes ?? 5
    };
    return this.http.post<ApiResponse<ShiftResponse>>(this.baseUrl, { ...request, ...payload });
  }

  /**
   * Actualizar turno existente
   * PUT /api/shifts/{id}
   */
  updateShift(id: string, request: WorkShiftRequest | CreateShiftRequest): Observable<ApiResponse<ShiftResponse>> {
    const payload: WorkShiftRequest = {
      code: request.code,
      name: request.name,
      description: request.description,
      startTime: request.startTime,
      endTime: request.endTime,
      crossesMidnight: request.crossesMidnight ?? (request as CreateShiftRequest).isOvernight ?? false,
      lateToleranceMinutes: request.lateToleranceMinutes ?? (request as CreateShiftRequest).gracePeriodMinutes ?? 10,
      earlyDepartureToleranceMinutes: request.earlyDepartureToleranceMinutes ?? 5
    };
    return this.http.put<ApiResponse<ShiftResponse>>(`${this.baseUrl}/${id}`, { ...request, ...payload });
  }

  /**
   * Activar turno
   * PATCH /api/shifts/{id}/activate
   */
  activateShift(id: string): Observable<ApiResponse<ShiftResponse>> {
    return this.http.patch<ApiResponse<ShiftResponse>>(`${this.baseUrl}/${id}/activate`, {});
  }

  /**
   * Desactivar turno
   * PATCH /api/shifts/{id}/deactivate
   */
  deactivateShift(id: string): Observable<ApiResponse<ShiftResponse>> {
    return this.http.patch<ApiResponse<ShiftResponse>>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  /**
   * Cambiar estado (compatible con UI actual que llama toggleShiftStatus)
   */
  toggleShiftStatus(id: string, newStatus: 'ACTIVE' | 'INACTIVE'): Observable<ApiResponse<ShiftResponse>> {
    if (newStatus === 'ACTIVE') {
      return this.activateShift(id);
    } else {
      return this.deactivateShift(id);
    }
  }

  // =========================================================================
  // 2. DESCANSOS DE TURNO (ShiftBreak - /api/shifts/{id}/breaks)
  // =========================================================================

  /**
   * Listar descansos de un turno
   * GET /api/shifts/{id}/breaks
   */
  listBreaks(
    shiftId: string,
    page = 0,
    size = 50,
    sort = 'startTime',
    direction = 'asc'
  ): Observable<ApiResponse<PageResponse<ShiftBreakResponse>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', Math.min(Math.max(1, size), 100).toString())
      .set('sort', sort)
      .set('direction', direction);

    return this.http.get<ApiResponse<PageResponse<ShiftBreakResponse>>>(
      `${this.baseUrl}/${shiftId}/breaks`,
      { params }
    );
  }

  /**
   * Consultar descanso por ID
   * GET /api/shifts/{id}/breaks/{breakId}
   */
  getBreakById(shiftId: string, breakId: string): Observable<ApiResponse<ShiftBreakResponse>> {
    return this.http.get<ApiResponse<ShiftBreakResponse>>(
      `${this.baseUrl}/${shiftId}/breaks/${breakId}`
    );
  }

  /**
   * Agregar descanso al turno
   * POST /api/shifts/{id}/breaks
   */
  createBreak(
    shiftId: string,
    request: ShiftBreakRequest
  ): Observable<ApiResponse<ShiftBreakResponse>> {
    return this.http.post<ApiResponse<ShiftBreakResponse>>(
      `${this.baseUrl}/${shiftId}/breaks`,
      request
    );
  }

  /**
   * Actualizar descanso
   * PUT /api/shifts/{id}/breaks/{breakId}
   */
  updateBreak(
    shiftId: string,
    breakId: string,
    request: ShiftBreakRequest
  ): Observable<ApiResponse<ShiftBreakResponse>> {
    return this.http.put<ApiResponse<ShiftBreakResponse>>(
      `${this.baseUrl}/${shiftId}/breaks/${breakId}`,
      request
    );
  }

  /**
   * Eliminar descanso
   * DELETE /api/shifts/{id}/breaks/{breakId}
   */
  deleteBreak(shiftId: string, breakId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/${shiftId}/breaks/${breakId}`
    );
  }

  // =========================================================================
  // 3. ASIGNACIONES A EMPLEADOS (/api/shifts/assignments)
  // =========================================================================

  /**
   * Crear asignación individual con días de semana
   * POST /api/shifts/assignments
   */
  createAssignment(request: CreateShiftAssignmentRequest): Observable<ApiResponse<ShiftAssignmentResponse>> {
    return this.http.post<ApiResponse<ShiftAssignmentResponse>>(
      `${this.baseUrl}/assignments`,
      request
    );
  }

  /**
   * Listar asignaciones
   * GET /api/shifts/assignments
   */
  listAssignments(
    employeeId?: string,
    shiftId?: string,
    status?: ShiftAssignmentStatus,
    page = 0,
    size = 50
  ): Observable<ApiResponse<PageResponse<ShiftAssignmentResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', Math.min(Math.max(1, size), 100).toString());

    if (employeeId) params = params.set('employeeId', employeeId);
    if (shiftId) params = params.set('shiftId', shiftId);
    if (status) params = params.set('status', status);

    return this.http.get<ApiResponse<PageResponse<ShiftAssignmentResponse>>>(
      `${this.baseUrl}/assignments`,
      { params }
    );
  }

  /**
   * Consultar asignación por ID
   * GET /api/shifts/assignments/{id}
   */
  getAssignmentById(id: string): Observable<ApiResponse<ShiftAssignmentResponse>> {
    return this.http.get<ApiResponse<ShiftAssignmentResponse>>(
      `${this.baseUrl}/assignments/${id}`
    );
  }

  /**
   * Actualizar asignación
   * PUT /api/shifts/assignments/{id}
   */
  updateAssignment(
    id: string,
    request: UpdateShiftAssignmentRequest
  ): Observable<ApiResponse<ShiftAssignmentResponse>> {
    return this.http.put<ApiResponse<ShiftAssignmentResponse>>(
      `${this.baseUrl}/assignments/${id}`,
      request
    );
  }

  /**
   * Cancelar asignación
   * PATCH /api/shifts/assignments/{id}/cancel
   */
  cancelAssignment(id: string): Observable<ApiResponse<ShiftAssignmentResponse>> {
    return this.http.patch<ApiResponse<ShiftAssignmentResponse>>(
      `${this.baseUrl}/assignments/${id}/cancel`,
      {}
    );
  }

  /**
   * Cerrar asignación
   * PATCH /api/shifts/assignments/{id}/close
   */
  closeAssignment(id: string): Observable<ApiResponse<ShiftAssignmentResponse>> {
    return this.http.patch<ApiResponse<ShiftAssignmentResponse>>(
      `${this.baseUrl}/assignments/${id}/close`,
      {}
    );
  }

  /**
   * Compatibilidad transicional: Asignación masiva en UI
   */
  assignShifts(request: AssignShiftRequest): Observable<ApiResponse<EmployeeShiftAssignmentResponse[]>> {
    return this.http.post<ApiResponse<EmployeeShiftAssignmentResponse[]>>(
      `${this.baseUrl}/assignments`,
      request
    );
  }

  /**
   * Compatibilidad transicional: Asignaciones de un empleado
   */
  getEmployeeAssignments(employeeId: string): Observable<ApiResponse<EmployeeShiftAssignmentResponse[]>> {
    return this.http.get<ApiResponse<EmployeeShiftAssignmentResponse[]>>(
      `${this.baseUrl}/assignments`,
      { params: new HttpParams().set('employeeId', employeeId) }
    );
  }

  // =========================================================================
  // 4. EXCEPCIONES DE CALENDARIO (/api/shifts/calendar-exceptions)
  // =========================================================================

  /**
   * Crear excepción de calendario
   * POST /api/shifts/calendar-exceptions
   */
  createCalendarException(
    request: WorkCalendarExceptionRequest
  ): Observable<ApiResponse<WorkCalendarExceptionResponse>> {
    return this.http.post<ApiResponse<WorkCalendarExceptionResponse>>(
      `${this.baseUrl}/calendar-exceptions`,
      request
    );
  }

  /**
   * Listar y filtrar excepciones de calendario
   * GET /api/shifts/calendar-exceptions
   */
  listCalendarExceptions(
    filter?: {
      employeeId?: string;
      departmentId?: string;
      fromDate?: string;
      toDate?: string;
      exceptionType?: CalendarExceptionType;
      workRequired?: boolean;
      organizationOnly?: boolean;
    },
    page = 0,
    size = 50
  ): Observable<ApiResponse<PageResponse<WorkCalendarExceptionResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', Math.min(Math.max(1, size), 100).toString());

    if (filter?.employeeId) params = params.set('employeeId', filter.employeeId);
    if (filter?.departmentId) params = params.set('departmentId', filter.departmentId);
    if (filter?.fromDate) params = params.set('fromDate', filter.fromDate);
    if (filter?.toDate) params = params.set('toDate', filter.toDate);
    if (filter?.exceptionType) params = params.set('exceptionType', filter.exceptionType);
    if (filter?.workRequired !== undefined) params = params.set('workRequired', filter.workRequired.toString());
    if (filter?.organizationOnly !== undefined) params = params.set('organizationOnly', filter.organizationOnly.toString());

    return this.http.get<ApiResponse<PageResponse<WorkCalendarExceptionResponse>>>(
      `${this.baseUrl}/calendar-exceptions`,
      { params }
    );
  }

  /**
   * Consultar excepción de calendario por ID
   * GET /api/shifts/calendar-exceptions/{id}
   */
  getCalendarExceptionById(id: string): Observable<ApiResponse<WorkCalendarExceptionResponse>> {
    return this.http.get<ApiResponse<WorkCalendarExceptionResponse>>(
      `${this.baseUrl}/calendar-exceptions/${id}`
    );
  }

  /**
   * Actualizar excepción de calendario
   * PUT /api/shifts/calendar-exceptions/{id}
   */
  updateCalendarException(
    id: string,
    request: WorkCalendarExceptionRequest
  ): Observable<ApiResponse<WorkCalendarExceptionResponse>> {
    return this.http.put<ApiResponse<WorkCalendarExceptionResponse>>(
      `${this.baseUrl}/calendar-exceptions/${id}`,
      request
    );
  }

  /**
   * Eliminar excepción de calendario
   * DELETE /api/shifts/calendar-exceptions/{id}
   */
  deleteCalendarException(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/calendar-exceptions/${id}`
    );
  }

  // =========================================================================
  // 5. HORARIO EFECTIVO RESUELTO (/api/shifts/schedule)
  // =========================================================================

  /**
   * Consultar horario efectivo resuelto para un empleado en una fecha
   * GET /api/shifts/schedule?employeeId={uuid}&date=YYYY-MM-DD
   */
  getEffectiveSchedule(employeeId: string, date: string): Observable<ApiResponse<ScheduleResponse>> {
    const params = new HttpParams()
      .set('employeeId', employeeId)
      .set('date', date);

    return this.http.get<ApiResponse<ScheduleResponse>>(
      `${this.baseUrl}/schedule`,
      { params }
    );
  }
}
