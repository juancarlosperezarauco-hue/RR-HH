import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { TenantAuditEventResponse, TenantAuditFilters } from '../model/tenant-audit.model';

@Injectable({
  providedIn: 'root'
})
export class TenantAuditService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/api/audit/events`;

  getAuditEvents(
    filters?: TenantAuditFilters,
    page = 0,
    size = 20
  ): Observable<ApiResponse<PageResponse<TenantAuditEventResponse>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters) {
      if (filters.startDate) {
        params = params.set('startDate', filters.startDate);
      }
      if (filters.endDate) {
        params = params.set('endDate', filters.endDate);
      }
      if (filters.actor && filters.actor.trim()) {
        params = params.set('actor', filters.actor.trim());
      }
      if (filters.module && filters.module !== 'all') {
        params = params.set('module', filters.module.trim());
      }
      if (filters.action && filters.action !== 'all') {
        params = params.set('action', filters.action.trim());
      }
      if (filters.resourceType && filters.resourceType !== 'all') {
        params = params.set('resourceType', filters.resourceType.trim());
      }
      if (filters.search && filters.search.trim()) {
        params = params.set('search', filters.search.trim());
      }
    }

    return this.http.get<ApiResponse<PageResponse<TenantAuditEventResponse>>>(this.API_URL, { params });
  }

  getAuditEventById(id: string): Observable<ApiResponse<TenantAuditEventResponse>> {
    return this.http.get<ApiResponse<TenantAuditEventResponse>>(`${this.API_URL}/${id}`);
  }
}
