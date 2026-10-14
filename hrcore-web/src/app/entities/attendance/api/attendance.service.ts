import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import {
  AttendanceRecordResponse,
  CalculateRecordRequest,
  ClockPunchRequest,
  AttendanceStatus,
  PunchSource
} from '../model/attendance-record.model';
import {
  AttendancePunchResponse,
  CreatePunchInput,
  PunchType
} from '../model/attendance-punch.model';
import {
  AttendanceAdjustmentResponse,
  CreateAdjustmentRequest,
  ReviewAdjustmentRequest,
  AttendanceOvertimeResponse,
  CreateOvertimeRequest,
  ApproveOvertimeRequest,
  AdjustmentType,
  ReviewStatus
} from '../model/attendance-adjustment.model';
import {
  AttendanceCorrectionResponse,
  RegularizeAttendanceRequest
} from '../model/attendance-correction.model';

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/attendance`;
  private readonly legacyBaseUrl = `${environment.apiUrl}/api/v1/attendance`;

  // ==========================================
  // HU17 - 1. PUNCHES (Marcaciones individuales)
  // ==========================================

  createPunch(input: CreatePunchInput): Observable<ApiResponse<AttendancePunchResponse>> {
    return this.http.post<ApiResponse<AttendancePunchResponse>>(`${this.baseUrl}/punches`, input);
  }

  listPunches(filters?: {
    employeeId?: string;
    recordId?: string;
    from?: string;
    to?: string;
    type?: PunchType;
    source?: PunchSource;
    page?: number;
    size?: number;
    sort?: string;
    direction?: string;
  }): Observable<ApiResponse<PageResponse<AttendancePunchResponse>>> {
    let params = new HttpParams();
    if (filters?.employeeId) params = params.set('employeeId', filters.employeeId);
    if (filters?.recordId) params = params.set('recordId', filters.recordId);
    if (filters?.from) params = params.set('from', filters.from);
    if (filters?.to) params = params.set('to', filters.to);
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.source) params = params.set('source', filters.source);
    params = params.set('page', (filters?.page ?? 0).toString());
    params = params.set('size', (filters?.size ?? 50).toString());
    params = params.set('sort', filters?.sort ?? 'punchedAt');
    params = params.set('direction', filters?.direction ?? 'desc');

    return this.http.get<ApiResponse<PageResponse<AttendancePunchResponse>>>(
      `${this.baseUrl}/punches`,
      { params }
    );
  }

  getPunchById(id: string): Observable<ApiResponse<AttendancePunchResponse>> {
    return this.http.get<ApiResponse<AttendancePunchResponse>>(`${this.baseUrl}/punches/${id}`);
  }

  // ==========================================
  // HU17 - 2. RECORDS (Jornadas Consolidadas)
  // ==========================================

  listRecords(
    startDateOrFrom: string,
    endDateOrTo: string,
    employeeId?: string,
    status?: AttendanceStatus | 'ALL',
    page = 0,
    size = 20,
    sort = 'workDate',
    direction = 'desc'
  ): Observable<ApiResponse<PageResponse<AttendanceRecordResponse>>> {
    let params = new HttpParams()
      .set('from', startDateOrFrom)
      .set('to', endDateOrTo)
      .set('startDate', startDateOrFrom)
      .set('endDate', endDateOrTo)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort)
      .set('direction', direction);

    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<AttendanceRecordResponse>>>(
      `${this.baseUrl}/records`,
      { params }
    ).pipe(map((res) => ({
      ...res,
      data: {
        ...res.data,
        content: (res.data?.content || []).map((record) => this.toViewRecord(record))
      }
    })));
  }

  getRecordById(id: string): Observable<ApiResponse<AttendanceRecordResponse>> {
    return this.http.get<ApiResponse<AttendanceRecordResponse>>(`${this.baseUrl}/records/${id}`).pipe(
      map((res) => ({ ...res, data: this.toViewRecord(res.data) }))
    );
  }

  calculateRecord(request: CalculateRecordRequest): Observable<ApiResponse<AttendanceRecordResponse>> {
    return this.http.post<ApiResponse<AttendanceRecordResponse>>(
      `${this.baseUrl}/records/calculate`,
      request
    ).pipe(map((res) => ({ ...res, data: this.toViewRecord(res.data) })));
  }

  getTodayStatus(employeeId?: string): Observable<ApiResponse<AttendanceRecordResponse | null>> {
    const today = new Date().toISOString().split('T')[0];
    let params = new HttpParams()
      .set('from', today)
      .set('to', today)
      .set('page', '0')
      .set('size', '1');

    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }

    return this.http.get<ApiResponse<PageResponse<AttendanceRecordResponse>>>(
      `${this.baseUrl}/records`,
      { params }
    ).pipe(
      map((res) => ({
        success: res.success,
        message: res.message,
        data: res.data?.content?.[0] ? this.toViewRecord(res.data.content[0]) : null,
        timestamp: res.timestamp
      }))
    );
  }

  // ==========================================
  // HU17 - 3. ADJUSTMENTS (Regularizaciones)
  // ==========================================

  createAdjustment(request: CreateAdjustmentRequest): Observable<ApiResponse<AttendanceAdjustmentResponse>> {
    return this.http.post<ApiResponse<AttendanceAdjustmentResponse>>(
      `${this.baseUrl}/adjustments`,
      request
    );
  }

  listAdjustments(filters?: {
    recordId?: string;
    requestedBy?: string;
    type?: AdjustmentType;
    status?: ReviewStatus;
    page?: number;
    size?: number;
    sort?: string;
    direction?: string;
  }): Observable<ApiResponse<PageResponse<AttendanceAdjustmentResponse>>> {
    let params = new HttpParams();
    if (filters?.recordId) params = params.set('recordId', filters.recordId);
    if (filters?.requestedBy) params = params.set('requestedBy', filters.requestedBy);
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.status) params = params.set('status', filters.status);
    params = params.set('page', (filters?.page ?? 0).toString());
    params = params.set('size', (filters?.size ?? 50).toString());
    params = params.set('sort', filters?.sort ?? 'createdAt');
    params = params.set('direction', filters?.direction ?? 'desc');

    return this.http.get<ApiResponse<PageResponse<AttendanceAdjustmentResponse>>>(
      `${this.baseUrl}/adjustments`,
      { params }
    );
  }

  getAdjustmentById(id: string): Observable<ApiResponse<AttendanceAdjustmentResponse>> {
    return this.http.get<ApiResponse<AttendanceAdjustmentResponse>>(`${this.baseUrl}/adjustments/${id}`);
  }

  approveAdjustment(id: string, review?: ReviewAdjustmentRequest): Observable<ApiResponse<AttendanceAdjustmentResponse>> {
    return this.http.patch<ApiResponse<AttendanceAdjustmentResponse>>(
      `${this.baseUrl}/adjustments/${id}/approve`,
      review ?? {}
    );
  }

  rejectAdjustment(id: string, review?: ReviewAdjustmentRequest): Observable<ApiResponse<AttendanceAdjustmentResponse>> {
    return this.http.patch<ApiResponse<AttendanceAdjustmentResponse>>(
      `${this.baseUrl}/adjustments/${id}/reject`,
      review ?? {}
    );
  }

  cancelAdjustment(id: string): Observable<ApiResponse<AttendanceAdjustmentResponse>> {
    return this.http.patch<ApiResponse<AttendanceAdjustmentResponse>>(
      `${this.baseUrl}/adjustments/${id}/cancel`,
      {}
    );
  }

  // ==========================================
  // HU17 - 4. OVERTIME (Horas Extraordinarias)
  // ==========================================

  createOvertime(request: CreateOvertimeRequest): Observable<ApiResponse<AttendanceOvertimeResponse>> {
    return this.http.post<ApiResponse<AttendanceOvertimeResponse>>(`${this.baseUrl}/overtime`, request);
  }

  listOvertime(filters?: {
    recordId?: string;
    status?: ReviewStatus;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
    sort?: string;
    direction?: string;
  }): Observable<ApiResponse<PageResponse<AttendanceOvertimeResponse>>> {
    let params = new HttpParams();
    if (filters?.recordId) params = params.set('recordId', filters.recordId);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.from) params = params.set('from', filters.from);
    if (filters?.to) params = params.set('to', filters.to);
    params = params.set('page', (filters?.page ?? 0).toString());
    params = params.set('size', (filters?.size ?? 50).toString());
    params = params.set('sort', filters?.sort ?? 'createdAt');
    params = params.set('direction', filters?.direction ?? 'desc');

    return this.http.get<ApiResponse<PageResponse<AttendanceOvertimeResponse>>>(
      `${this.baseUrl}/overtime`,
      { params }
    );
  }

  getOvertimeById(id: string): Observable<ApiResponse<AttendanceOvertimeResponse>> {
    return this.http.get<ApiResponse<AttendanceOvertimeResponse>>(`${this.baseUrl}/overtime/${id}`);
  }

  approveOvertime(id: string, request: ApproveOvertimeRequest): Observable<ApiResponse<AttendanceOvertimeResponse>> {
    return this.http.patch<ApiResponse<AttendanceOvertimeResponse>>(
      `${this.baseUrl}/overtime/${id}/approve`,
      request
    );
  }

  rejectOvertime(id: string, review?: { comment?: string }): Observable<ApiResponse<AttendanceOvertimeResponse>> {
    return this.http.patch<ApiResponse<AttendanceOvertimeResponse>>(
      `${this.baseUrl}/overtime/${id}/reject`,
      review ?? {}
    );
  }

  cancelOvertime(id: string): Observable<ApiResponse<AttendanceOvertimeResponse>> {
    return this.http.patch<ApiResponse<AttendanceOvertimeResponse>>(
      `${this.baseUrl}/overtime/${id}/cancel`,
      {}
    );
  }

  // ==========================================
  // MÉTODOS DE COMPATIBILIDAD HACIA ATRÁS
  // ==========================================

  /**
   * Registro canónico de marcación: POST /api/attendance/punches
   */
  registerClock(request: ClockPunchRequest): Observable<ApiResponse<any>> {
    let canonicalType: PunchType = 'IN';
    if (request.type === 'OUT') {
      canonicalType = 'OUT';
    } else if (request.type === 'BREAK_START' || request.type === 'BREAK_OUT') {
      canonicalType = 'BREAK_START';
    } else if (request.type === 'BREAK_END' || request.type === 'BREAK_IN') {
      canonicalType = 'BREAK_END';
    }

    const payload: CreatePunchInput = {
      employeeId: request.employeeId || undefined,
      punchType: canonicalType,
      punchedAt: new Date().toISOString(),
      latitude: request.latitude,
      longitude: request.longitude,
      notes: request.notes
    };

    return this.createPunch(payload);
  }

  /**
   * Regularización canónica: POST /api/attendance/adjustments
   */
  regularizeAttendance(request: RegularizeAttendanceRequest): Observable<ApiResponse<any>> {
    return this.createAdjustment({
      attendanceRecordId: request.attendanceRecordId,
      adjustmentType: 'TIME_CORRECTION',
      reason: request.reason,
      requestedValues: {
        checkInTime: request.correctedCheckIn,
        checkOutTime: request.correctedCheckOut
      }
    });
  }

  private toViewRecord(record: AttendanceRecordResponse): AttendanceRecordResponse {
    return {
      ...record,
      checkInTime: record.checkInTime ?? record.actualFirstInAt ?? null,
      checkOutTime: record.checkOutTime ?? record.actualLastOutAt ?? null,
      workedHours: record.workedHours ?? Number(((record.workedMinutes ?? 0) / 60).toFixed(2))
    };
  }
}
