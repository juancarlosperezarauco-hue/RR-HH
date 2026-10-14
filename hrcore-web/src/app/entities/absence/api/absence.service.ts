import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import {
  LeaveTypeResponse,
  CreateLeaveTypeInput
} from '../model/leave-type.model';
import {
  LeaveBalanceResponse,
  CreateLeaveBalanceInput,
  AdjustLeaveBalanceInput
} from '../model/leave-balance.model';
import {
  LeaveRequestResponse,
  CreateLeaveRequestInput,
  CreateLeaveRequestPayload,
  ReviewLeaveRequestPayload,
  LeaveAttachmentResponse,
  LeaveHistoryResponse,
  LeaveStatus
} from '../model/leave-request.model';
import { HolidayResponse } from '../model/holiday.model';

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/leaves`;
  private readonly legacyBaseUrl = `${environment.apiUrl}/api/v1`;

  // ==========================================
  // HU18 - 1. LEAVE TYPES (Catálogo de Tipos)
  // ==========================================

  getLeaveTypes(): Observable<ApiResponse<LeaveTypeResponse[]>> {
    return this.http.get<ApiResponse<LeaveTypeResponse[]>>(`${this.baseUrl}/types`);
  }

  getLeaveTypeById(id: string): Observable<ApiResponse<LeaveTypeResponse>> {
    return this.http.get<ApiResponse<LeaveTypeResponse>>(`${this.baseUrl}/types/${id}`);
  }

  createLeaveType(input: CreateLeaveTypeInput): Observable<ApiResponse<LeaveTypeResponse>> {
    return this.http.post<ApiResponse<LeaveTypeResponse>>(`${this.baseUrl}/types`, input);
  }

  updateLeaveType(id: string, input: CreateLeaveTypeInput): Observable<ApiResponse<LeaveTypeResponse>> {
    return this.http.put<ApiResponse<LeaveTypeResponse>>(`${this.baseUrl}/types/${id}`, input);
  }

  activateLeaveType(id: string): Observable<ApiResponse<LeaveTypeResponse>> {
    return this.http.patch<ApiResponse<LeaveTypeResponse>>(`${this.baseUrl}/types/${id}/activate`, {});
  }

  deactivateLeaveType(id: string): Observable<ApiResponse<LeaveTypeResponse>> {
    return this.http.patch<ApiResponse<LeaveTypeResponse>>(`${this.baseUrl}/types/${id}/deactivate`, {});
  }

  // ==========================================
  // HU18 - 2. LEAVE BALANCES (Saldos y Ajustes)
  // ==========================================

  getEmployeeBalance(employeeId: string, _year?: number): Observable<ApiResponse<LeaveBalanceResponse>> {
    return this.http.get<ApiResponse<LeaveBalanceResponse>>(`${this.legacyBaseUrl}/leaves/balances/${employeeId}`);
  }

  listBalances(filters?: {
    employeeId?: string;
    leaveTypeId?: string;
    year?: number;
    page?: number;
    size?: number;
  }): Observable<ApiResponse<PageResponse<LeaveBalanceResponse>>> {
    let params = new HttpParams();
    if (filters?.employeeId) params = params.set('employeeId', filters.employeeId);
    if (filters?.leaveTypeId) params = params.set('leaveTypeId', filters.leaveTypeId);
    if (filters?.year) params = params.set('year', filters.year.toString());
    params = params.set('page', (filters?.page ?? 0).toString());
    params = params.set('size', (filters?.size ?? 25).toString());

    return this.http.get<ApiResponse<PageResponse<LeaveBalanceResponse>>>(
      `${this.baseUrl}/balances`,
      { params }
    );
  }

  getBalanceById(id: string): Observable<ApiResponse<LeaveBalanceResponse>> {
    return this.http.get<ApiResponse<LeaveBalanceResponse>>(`${this.baseUrl}/balances/${id}`);
  }

  createBalance(input: CreateLeaveBalanceInput): Observable<ApiResponse<LeaveBalanceResponse>> {
    return this.http.post<ApiResponse<LeaveBalanceResponse>>(`${this.baseUrl}/balances`, input);
  }

  adjustBalance(id: string, input: AdjustLeaveBalanceInput): Observable<ApiResponse<LeaveBalanceResponse>> {
    return this.http.patch<ApiResponse<LeaveBalanceResponse>>(`${this.baseUrl}/balances/${id}/adjust`, input);
  }

  // ==========================================
  // HU18 - 3. LEAVE REQUESTS (Solicitudes)
  // ==========================================

  listRequests(
    employeeId?: string,
    status?: LeaveStatus | 'ALL',
    page = 0,
    size = 20,
    from?: string,
    to?: string,
    leaveTypeId?: string
  ): Observable<ApiResponse<PageResponse<LeaveRequestResponse>>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    if (leaveTypeId) {
      params = params.set('leaveTypeId', leaveTypeId);
    }

    return this.http.get<ApiResponse<PageResponse<LeaveRequestResponse>>>(
      `${this.baseUrl}/requests`,
      { params }
    );
  }

  getRequestById(id: string): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.get<ApiResponse<LeaveRequestResponse>>(`${this.baseUrl}/requests/${id}`);
  }

  createRequest(input: CreateLeaveRequestInput): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.post<ApiResponse<LeaveRequestResponse>>(`${this.baseUrl}/requests`, input);
  }

  updateRequest(id: string, input: CreateLeaveRequestInput): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.put<ApiResponse<LeaveRequestResponse>>(`${this.baseUrl}/requests/${id}`, input);
  }

  submitRequestDraft(id: string): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.patch<ApiResponse<LeaveRequestResponse>>(`${this.baseUrl}/requests/${id}/submit`, {});
  }

  approveRequest(id: string, comment?: string): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.patch<ApiResponse<LeaveRequestResponse>>(
      `${this.baseUrl}/requests/${id}/approve`,
      { comment: comment ?? null }
    );
  }

  rejectRequest(id: string, comment?: string): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.patch<ApiResponse<LeaveRequestResponse>>(
      `${this.baseUrl}/requests/${id}/reject`,
      { comment: comment ?? null }
    );
  }

  cancelRequest(id: string, comment?: string): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.patch<ApiResponse<LeaveRequestResponse>>(
      `${this.baseUrl}/requests/${id}/cancel`,
      { comment: comment ?? null }
    );
  }

  getRequestHistory(
    id: string,
    page = 0,
    size = 25
  ): Observable<ApiResponse<PageResponse<LeaveHistoryResponse>>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<LeaveHistoryResponse>>>(
      `${this.baseUrl}/requests/${id}/history`,
      { params }
    );
  }

  // ==========================================
  // HU18 - 4. ATTACHMENTS (Comprobantes)
  // ==========================================

  uploadAttachment(requestId: string, file: File): Observable<ApiResponse<LeaveAttachmentResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<LeaveAttachmentResponse>>(
      `${this.baseUrl}/requests/${requestId}/attachments`,
      formData
    );
  }

  listAttachments(
    requestId: string,
    page = 0,
    size = 25
  ): Observable<ApiResponse<PageResponse<LeaveAttachmentResponse>>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<PageResponse<LeaveAttachmentResponse>>>(
      `${this.baseUrl}/requests/${requestId}/attachments`,
      { params }
    );
  }

  downloadAttachment(requestId: string, attachmentId: string): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/requests/${requestId}/attachments/${attachmentId}/download`,
      { responseType: 'blob' }
    );
  }

  // ==========================================
  // MÉTODOS DE COMPATIBILIDAD HACIA ATRÁS
  // ==========================================

  submitRequest(payload: CreateLeaveRequestPayload): Observable<ApiResponse<LeaveRequestResponse>> {
    const formData = new FormData();
    formData.append('employeeId', payload.employeeId);
    formData.append('leaveTypeId', payload.leaveTypeId);
    formData.append('startDate', payload.startDate);
    formData.append('endDate', payload.endDate);
    formData.append('reason', payload.reason);
    if (payload.proofDocument) {
      formData.append('proofDocument', payload.proofDocument);
    }

    return this.http.post<ApiResponse<LeaveRequestResponse>>(
      `${this.legacyBaseUrl}/leaves/requests`,
      formData
    );
  }

  reviewRequest(payload: ReviewLeaveRequestPayload): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.patch<ApiResponse<LeaveRequestResponse>>(
      `${this.legacyBaseUrl}/leaves/requests/${payload.requestId}/review`,
      {
        approved: payload.approved,
        rejectionReason: payload.rejectionReason || null
      }
    );
  }

  getHolidays(): Observable<ApiResponse<HolidayResponse[]>> {
    return this.http.get<ApiResponse<HolidayResponse[]>>(`${this.legacyBaseUrl}/holidays`);
  }
}
