import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../shared/api';
import { LoginRequest } from '../model/login-request.model';
import { AuthTokenResponse } from '../model/auth-token-response.model';
import { AuthenticatedTenantUserResponse } from '../model/authenticated-tenant-user-response.model';
import { FaceLoginRequest } from '../model/face-login-request.model';
import { TenantProfileResponse } from '../model/tenant-profile-response.model';
import { UpdateTenantProfileRequest } from '../model/update-tenant-profile-request.model';
import { ForgotPasswordRequest } from '../model/forgot-password-request.model';
import { ResetPasswordRequest } from '../model/reset-password-request.model';
import { ActivateAccountRequest } from '../model/activate-account-request.model';
import { ResendActivationRequest } from '../model/resend-activation-request.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/api/auth`;

  login(request: LoginRequest): Observable<ApiResponse<AuthTokenResponse>> {
    // Si la petición ya trae un tenantSlug (ingresado en el formulario o en storage pero no inyectado aún), lo enviamos explícitamente en el header
    const headersConfig: { [key: string]: string } = {};
    if (request.tenantSlug) {
      headersConfig['X-Tenant-Slug'] = request.tenantSlug;
    }

    const headers = new HttpHeaders(headersConfig);

    // No enviamos tenantSlug en el body porque la API probablemente no lo espera allí
    const body = {
      email: request.email,
      password: request.password
    };

    return this.http.post<ApiResponse<AuthTokenResponse>>(`${this.API_URL}/login`, body, { headers });
  }

  faceLogin(request: FaceLoginRequest): Observable<ApiResponse<AuthTokenResponse>> {
    const headers = new HttpHeaders({
      'X-Tenant-Slug': request.tenantSlug
    });

    const formData = new FormData();
    formData.append('email', request.email);
    formData.append('image', request.image, request.image.name || 'face.jpg');

    return this.http.post<ApiResponse<AuthTokenResponse>>(`${this.API_URL}/face/login`, formData, { headers });
  }

  getMe(): Observable<ApiResponse<AuthenticatedTenantUserResponse>> {
    return this.http.get<ApiResponse<AuthenticatedTenantUserResponse>>(`${this.API_URL}/me`);
  }

  getProfile(): Observable<ApiResponse<TenantProfileResponse>> {
    return this.http.get<ApiResponse<TenantProfileResponse>>(`${this.API_URL}/profile`);
  }

  updateProfile(request: UpdateTenantProfileRequest): Observable<ApiResponse<TenantProfileResponse>> {
    const formData = new FormData();

    if (request.firstName != null) {
      formData.append('firstName', request.firstName);
    }

    if (request.lastName != null) {
      formData.append('lastName', request.lastName);
    }

    if (request.photo) {
      formData.append('photo', request.photo, request.photo.name || 'profile.jpg');
    }

    return this.http.put<ApiResponse<TenantProfileResponse>>(`${this.API_URL}/profile`, formData);
  }

  updateProfilePhoto(photo: File): Observable<ApiResponse<TenantProfileResponse>> {
    const formData = new FormData();
    formData.append('photo', photo, photo.name || 'profile.jpg');
    return this.http.put<ApiResponse<TenantProfileResponse>>(`${this.API_URL}/profile/photo`, formData);
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<Record<string, string>>> {
    const headers = new HttpHeaders({
      'X-Tenant-Slug': request.tenantSlug
    });

    return this.http.post<ApiResponse<Record<string, string>>>(
      `${this.API_URL}/forgot-password`,
      { email: request.email },
      { headers }
    );
  }

  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<Record<string, string>>> {
    const headers = new HttpHeaders({
      'X-Tenant-Slug': request.tenantSlug
    });

    return this.http.post<ApiResponse<Record<string, string>>>(
      `${this.API_URL}/reset-password`,
      {
        token: request.token,
        newPassword: request.newPassword
      },
      { headers }
    );
  }

  activateAccount(request: ActivateAccountRequest): Observable<ApiResponse<Record<string, string>>> {
    const headers = new HttpHeaders({
      'X-Tenant-Slug': request.tenantSlug
    });

    return this.http.post<ApiResponse<Record<string, string>>>(
      `${this.API_URL}/activate-account`,
      { token: request.token },
      { headers }
    );
  }

  resendActivation(request: ResendActivationRequest): Observable<ApiResponse<Record<string, string>>> {
    const headers = new HttpHeaders({
      'X-Tenant-Slug': request.tenantSlug
    });

    return this.http.post<ApiResponse<Record<string, string>>>(
      `${this.API_URL}/resend-activation`,
      { email: request.email },
      { headers }
    );
  }

  removeProfilePhoto(): Observable<ApiResponse<TenantProfileResponse>> {
    return this.http.delete<ApiResponse<TenantProfileResponse>>(`${this.API_URL}/profile/photo`);
  }
}
