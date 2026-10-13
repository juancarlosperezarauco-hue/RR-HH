import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { LeaveTypeResponse } from '../model/leave-type.model';
import { LeaveBalanceResponse } from '../model/leave-balance.model';
import {
  LeaveRequestResponse,
  CreateLeaveRequestPayload,
  ReviewLeaveRequestPayload,
  LeaveStatus
} from '../model/leave-request.model';
import { HolidayResponse } from '../model/holiday.model';

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  getLeaveTypes(): Observable<ApiResponse<LeaveTypeResponse[]>> {
    return this.http.get<ApiResponse<LeaveTypeResponse[]>>(`${this.baseUrl}/leaves/types`);
  }

  getEmployeeBalance(employeeId: string): Observable<ApiResponse<LeaveBalanceResponse>> {
    return this.http.get<ApiResponse<LeaveBalanceResponse>>(`${this.baseUrl}/leaves/balances/${employeeId}`);
  }

  listRequests(
    employeeId?: string,
    status?: LeaveStatus | 'ALL',
    page = 0,
    size = 20
  ): Observable<ApiResponse<PageResponse<LeaveRequestResponse>>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<LeaveRequestResponse>>>(`${this.baseUrl}/leaves/requests`, { params });
  }

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

    return this.http.post<ApiResponse<LeaveRequestResponse>>(`${this.baseUrl}/leaves/requests`, formData);
  }

  reviewRequest(payload: ReviewLeaveRequestPayload): Observable<ApiResponse<LeaveRequestResponse>> {
    return this.http.patch<ApiResponse<LeaveRequestResponse>>(
      `${this.baseUrl}/leaves/requests/${payload.requestId}/review`,
      {
        approved: payload.approved,
        rejectionReason: payload.rejectionReason || null
      }
    );
  }

  getHolidays(): Observable<ApiResponse<HolidayResponse[]>> {
    return this.http.get<ApiResponse<HolidayResponse[]>>(`${this.baseUrl}/holidays`);
  }
}
