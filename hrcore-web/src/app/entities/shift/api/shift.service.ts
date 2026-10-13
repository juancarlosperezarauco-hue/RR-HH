import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { ShiftResponse, CreateShiftRequest } from '../model/shift.model';
import {
  EmployeeShiftAssignmentResponse,
  AssignShiftRequest
} from '../model/shift-assignment.model';

@Injectable({ providedIn: 'root' })
export class ShiftService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/shifts`;

  listShifts(
    status?: 'ACTIVE' | 'INACTIVE' | 'ALL',
    page = 0,
    size = 20
  ): Observable<ApiResponse<PageResponse<ShiftResponse>>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<ShiftResponse>>>(this.baseUrl, { params });
  }

  createShift(request: CreateShiftRequest): Observable<ApiResponse<ShiftResponse>> {
    return this.http.post<ApiResponse<ShiftResponse>>(this.baseUrl, request);
  }

  updateShift(id: string, request: CreateShiftRequest): Observable<ApiResponse<ShiftResponse>> {
    return this.http.put<ApiResponse<ShiftResponse>>(`${this.baseUrl}/${id}`, request);
  }

  toggleShiftStatus(id: string, newStatus: 'ACTIVE' | 'INACTIVE'): Observable<ApiResponse<ShiftResponse>> {
    return this.http.patch<ApiResponse<ShiftResponse>>(`${this.baseUrl}/${id}/status`, { status: newStatus });
  }

  assignShifts(request: AssignShiftRequest): Observable<ApiResponse<EmployeeShiftAssignmentResponse[]>> {
    return this.http.post<ApiResponse<EmployeeShiftAssignmentResponse[]>>(`${this.baseUrl}/assignments`, request);
  }

  getEmployeeAssignments(employeeId: string): Observable<ApiResponse<EmployeeShiftAssignmentResponse[]>> {
    return this.http.get<ApiResponse<EmployeeShiftAssignmentResponse[]>>(
      `${this.baseUrl}/assignments/employee/${employeeId}`
    );
  }
}
