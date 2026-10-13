import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import {
  AttendanceRecordResponse,
  ClockPunchRequest,
  AttendanceStatus
} from '../model/attendance-record.model';
import {
  AttendanceCorrectionResponse,
  RegularizeAttendanceRequest
} from '../model/attendance-correction.model';

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/attendance`;

  listRecords(
    startDate: string,
    endDate: string,
    employeeId?: string,
    status?: AttendanceStatus | 'ALL',
    page = 0,
    size = 20
  ): Observable<ApiResponse<PageResponse<AttendanceRecordResponse>>> {
    let params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());

    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }
    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<AttendanceRecordResponse>>>(`${this.baseUrl}/records`, { params });
  }

  registerClock(request: ClockPunchRequest): Observable<ApiResponse<AttendanceRecordResponse>> {
    return this.http.post<ApiResponse<AttendanceRecordResponse>>(`${this.baseUrl}/clock`, request);
  }

  regularizeAttendance(request: RegularizeAttendanceRequest): Observable<ApiResponse<AttendanceCorrectionResponse>> {
    return this.http.post<ApiResponse<AttendanceCorrectionResponse>>(`${this.baseUrl}/corrections`, request);
  }

  getTodayStatus(employeeId: string): Observable<ApiResponse<AttendanceRecordResponse | null>> {
    return this.http.get<ApiResponse<AttendanceRecordResponse | null>>(`${this.baseUrl}/today/${employeeId}`);
  }
}
