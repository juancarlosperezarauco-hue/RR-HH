import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { TenantDashboardResponse } from '../model/tenant-dashboard-response.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/dashboard`;

  getTenantDashboard(fromDate?: string, toDate?: string): Observable<ApiResponse<TenantDashboardResponse>> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    return this.http.get<ApiResponse<TenantDashboardResponse>>(`${this.apiUrl}/summary`, { params });
  }
}
