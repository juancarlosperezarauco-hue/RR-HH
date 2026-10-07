import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../shared/api';
import { PageResponse, TenantUserResponse } from '../model/tenant-user-response.model';
import { CreateTenantUserRequest } from '../model/create-tenant-user-request.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/api/users`;

  private buildPageParams(page = 0, size = 20): HttpParams {
    return new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
  }

  getUsers(page = 0, size = 20): Observable<ApiResponse<PageResponse<TenantUserResponse>>> {
    return this.http.get<ApiResponse<PageResponse<TenantUserResponse>>>(
      this.API_URL,
      { params: this.buildPageParams(page, size) }
    );
  }

  getUserById(id: string): Observable<ApiResponse<TenantUserResponse>> {
    return this.http.get<ApiResponse<TenantUserResponse>>(`${this.API_URL}/${id}`);
  }

  createUser(request: CreateTenantUserRequest): Observable<ApiResponse<TenantUserResponse>> {
    return this.http.post<ApiResponse<TenantUserResponse>>(this.API_URL, request);
  }

  updateUser(id: string, request: any): Observable<ApiResponse<TenantUserResponse>> {
    return this.http.put<ApiResponse<TenantUserResponse>>(`${this.API_URL}/${id}`, request);
  }

  activateUser(id: string): Observable<ApiResponse<TenantUserResponse>> {
    return this.http.patch<ApiResponse<TenantUserResponse>>(`${this.API_URL}/${id}/activate`, {});
  }

  deactivateUser(id: string): Observable<ApiResponse<TenantUserResponse>> {
    return this.http.patch<ApiResponse<TenantUserResponse>>(`${this.API_URL}/${id}/deactivate`, {});
  }
}
