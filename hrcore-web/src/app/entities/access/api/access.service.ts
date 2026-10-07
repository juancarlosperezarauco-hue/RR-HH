import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../model/page-response.model';
import { SystemPermissionResponse } from '../model/system-permission-response.model';
import { CreateTenantRoleRequest, TenantRoleResponse, UpdateTenantRoleRequest } from '../model/tenant-role.model';
import { AssignUserRolesRequest, UserRolesResponse } from '../model/user-roles.model';

@Injectable({
  providedIn: 'root'
})
export class AccessService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/api/access`;

  // --- Permissions ---
  getPermissions(page = 0, size = 200): Observable<ApiResponse<PageResponse<SystemPermissionResponse>>> {
    return this.http.get<ApiResponse<PageResponse<SystemPermissionResponse>>>(`${this.API_URL}/permissions?page=${page}&size=${size}`);
  }

  // --- Roles ---
  getRoles(page = 0, size = 200): Observable<ApiResponse<PageResponse<TenantRoleResponse>>> {
    return this.http.get<ApiResponse<PageResponse<TenantRoleResponse>>>(`${this.API_URL}/roles?page=${page}&size=${size}`);
  }

  getRoleById(id: string): Observable<ApiResponse<TenantRoleResponse>> {
    return this.http.get<ApiResponse<TenantRoleResponse>>(`${this.API_URL}/roles/${id}`);
  }

  createRole(request: CreateTenantRoleRequest): Observable<ApiResponse<TenantRoleResponse>> {
    return this.http.post<ApiResponse<TenantRoleResponse>>(`${this.API_URL}/roles`, request);
  }

  updateRole(id: string, request: UpdateTenantRoleRequest): Observable<ApiResponse<TenantRoleResponse>> {
    return this.http.put<ApiResponse<TenantRoleResponse>>(`${this.API_URL}/roles/${id}`, request);
  }

  activateRole(id: string): Observable<ApiResponse<TenantRoleResponse>> {
    return this.http.patch<ApiResponse<TenantRoleResponse>>(`${this.API_URL}/roles/${id}/activate`, {});
  }

  deactivateRole(id: string): Observable<ApiResponse<TenantRoleResponse>> {
    return this.http.patch<ApiResponse<TenantRoleResponse>>(`${this.API_URL}/roles/${id}/deactivate`, {});
  }

  // --- User Roles Assignment ---
  getUserRoles(userId: number | string): Observable<ApiResponse<UserRolesResponse>> {
    return this.http.get<ApiResponse<UserRolesResponse>>(`${this.API_URL}/users/${userId}/roles`);
  }

  assignUserRoles(userId: number | string, request: AssignUserRolesRequest): Observable<ApiResponse<UserRolesResponse>> {
    return this.http.put<ApiResponse<UserRolesResponse>>(`${this.API_URL}/users/${userId}/roles`, request);
  }
}
