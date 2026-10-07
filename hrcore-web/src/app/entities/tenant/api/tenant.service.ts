import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../shared/api';
import { PublicSignupRequest } from '../model/public-signup-request.model';
import { PublicPaidSignupRequest } from '../model/public-paid-signup-request.model';
import { PublicPaidSignupResponse } from '../model/public-paid-signup-response.model';
import { PublicSignupResponse } from '../model/public-signup-response.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TenantService {
  private http = inject(HttpClient);
  // Reemplazar con la ruta de tu entorno real
  private readonly API_URL = `${environment.apiUrl}/api/public`; 

  publicSignup(request: PublicSignupRequest): Observable<ApiResponse<PublicSignupResponse>> {
    return this.http.post<ApiResponse<PublicSignupResponse>>(`${this.API_URL}/signup`, request);
  }

  publicPaidSignup(request: PublicPaidSignupRequest): Observable<ApiResponse<PublicPaidSignupResponse>> {
    return this.http.post<ApiResponse<PublicPaidSignupResponse>>(`${this.API_URL}/signup/checkout`, request);
  }
}
