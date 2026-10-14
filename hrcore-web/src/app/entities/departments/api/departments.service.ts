import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../access';
import { Department, DepartmentFilters, DepartmentHistory, DepartmentRequest } from '../model/departments.model';

@Injectable({ providedIn: 'root' })
export class DepartmentsService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/departments`;
  list(page = 0, size = 25, filters: Partial<DepartmentFilters> = {}, sort = 'name,asc') {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    for (const [key, value] of Object.entries(filters)) if (value?.trim()) params = params.set(key, value.trim());
    return this.http.get<ApiResponse<PageResponse<Department>>>(this.api, { params }).pipe(map(checked));
  }
  get(id: string) { return this.http.get<ApiResponse<Department>>(`${this.api}/${id}`).pipe(map(checked)); }
  create(request: DepartmentRequest) { return this.http.post<ApiResponse<Department>>(this.api, request).pipe(map(checked)); }
  update(id: string, request: DepartmentRequest) { return this.http.put<ApiResponse<Department>>(`${this.api}/${id}`, request).pipe(map(checked)); }
  changeStatus(id: string, activate: boolean, reason: string | null) {
    return this.http.patch<ApiResponse<Department>>(`${this.api}/${id}/${activate ? 'activate' : 'deactivate'}`, { reason }).pipe(map(checked));
  }
  history(id: string, page = 0) {
    return this.http.get<ApiResponse<PageResponse<DepartmentHistory>>>(`${this.api}/${id}/history`, {
      params: new HttpParams().set('page', page).set('size', 25)
    }).pipe(map(checked));
  }
}
function checked<T>(response: ApiResponse<T>): ApiResponse<T> {
  if (!response.success) throw new Error(response.message || 'La operación no se completó.');
  return response;
}
