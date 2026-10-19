import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';

export type HrReportId = 'employees' | 'departments' | 'positions' | 'contracts' | 'files' | 'shifts' | 'attendance' | 'leaves' | 'incidents' | 'integrated';

export interface HrReportFilters {
  fromDate?: string;
  toDate?: string;
  departmentId?: string;
  positionId?: string;
  employeeId?: string;
  employmentPeriodId?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/hr/reports`;

  get(type: HrReportId, filters: HrReportFilters = {}) {
    return this.http.get<ApiResponse<Record<string, unknown>>>(`${this.api}/${type}`, {
      params: this.params(filters)
    });
  }

  export(type: HrReportId, format: 'xlsx' | 'docx' | 'pdf', filters: HrReportFilters = {}) {
    return this.http.get(`${this.api}/${type}/export/${format}`, {
      params: this.params(filters),
      responseType: 'blob'
    });
  }

  private params(filters: HrReportFilters) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value?.trim()) params = params.set(key, value.trim());
    }
    return params;
  }
}
