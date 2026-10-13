import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../access';
import {
  AssignmentOption, ContractHistory, ContractPage, ContractRenewalRequest, ContractRequest,
  ContractStatus, DepartmentOption, EmployeeOption, EmploymentContract, PositionOption
} from '../model/contracts.model';

@Injectable({ providedIn: 'root' })
export class ContractsService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api`;

  list(page = 0, size = 25, search = '', status = ''): Observable<ApiResponse<ContractPage>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    if (search.trim()) params = params.set('search', search.trim());
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<ContractPage>>(`${this.api}/contracts`, { params });
  }
  get(id: string): Observable<ApiResponse<EmploymentContract>> {
    return this.http.get<ApiResponse<EmploymentContract>>(`${this.api}/contracts/${id}`);
  }
  create(request: ContractRequest): Observable<ApiResponse<EmploymentContract>> {
    return this.http.post<ApiResponse<EmploymentContract>>(`${this.api}/contracts`, request);
  }
  update(id: string, request: ContractRequest): Observable<ApiResponse<EmploymentContract>> {
    return this.http.put<ApiResponse<EmploymentContract>>(`${this.api}/contracts/${id}`, request);
  }
  changeStatus(id: string, status: ContractStatus, reason: string | null = null, effectiveDate: string | null = null): Observable<ApiResponse<EmploymentContract>> {
    return this.http.patch<ApiResponse<EmploymentContract>>(`${this.api}/contracts/${id}/status`, { status, reason, effectiveDate });
  }
  renew(id: string, request: ContractRenewalRequest): Observable<ApiResponse<EmploymentContract>> {
    return this.http.post<ApiResponse<EmploymentContract>>(`${this.api}/contracts/${id}/renew`, request);
  }
  history(id: string): Observable<ApiResponse<ContractHistory[]>> {
    return this.http.get<ApiResponse<ContractHistory[]>>(`${this.api}/contracts/${id}/history`);
  }
  document(id: string): Observable<Blob> {
    return this.http.get(`${this.api}/contracts/${id}/document`, { responseType: 'blob' });
  }
  employees(): Observable<ApiResponse<PageResponse<EmployeeOption>>> {
    return this.http.get<ApiResponse<PageResponse<EmployeeOption>>>(`${this.api}/employees?page=0&size=100&sort=lastName,asc`);
  }
  assignments(employeeId: string): Observable<ApiResponse<PageResponse<AssignmentOption>>> {
    return this.http.get<ApiResponse<PageResponse<AssignmentOption>>>(`${this.api}/employees/${employeeId}/assignments?page=0&size=100&sort=startDate,desc`);
  }
  departments(): Observable<ApiResponse<PageResponse<DepartmentOption>>> {
    return this.http.get<ApiResponse<PageResponse<DepartmentOption>>>(`${this.api}/departments?page=0&size=100&sort=name,asc`);
  }
  positions(): Observable<ApiResponse<PageResponse<PositionOption>>> {
    return this.http.get<ApiResponse<PageResponse<PositionOption>>>(`${this.api}/positions?page=0&size=100&sort=name,asc`);
  }
}
