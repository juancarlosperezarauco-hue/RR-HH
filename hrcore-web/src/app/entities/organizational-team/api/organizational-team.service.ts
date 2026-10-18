import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../shared/api';
import { OrganizationChart, TeamEmployee } from '../model/organizational-team.model';

@Injectable({ providedIn: 'root' })
export class OrganizationalTeamService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/my-team/employees`;
  list(page = 0, size = 25, sort = 'lastName,asc') {
    return this.http.get<ApiResponse<PageResponse<TeamEmployee>>>(this.api, {
      params: new HttpParams().set('page', page).set('size', size).set('sort', sort)
    }).pipe(map(checked));
  }
  chart() {
    return this.http.get<ApiResponse<OrganizationChart>>(`${environment.apiUrl}/api/organization/chart`).pipe(map(checked));
  }
}
function checked<T>(response: ApiResponse<T>): ApiResponse<T> {
  if (!response.success) throw new Error(response.message || 'La operación no se completó.');
  return response;
}
