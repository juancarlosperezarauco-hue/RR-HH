import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PlatformSubscription } from '../../../entities/platform/api/platform.service';
import {
  TenantBillingPlan,
  TenantCheckoutResultResponse,
  TenantCheckoutSessionRequest,
  TenantCheckoutSessionResponse
} from '../model/tenant-billing.model';

@Injectable({
  providedIn: 'root'
})
export class TenantBillingService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/api/subscription`;

  getCurrentSubscription(): Observable<ApiResponse<PlatformSubscription>> {
    return this.http.get<ApiResponse<PlatformSubscription>>(`${this.API_URL}/current`);
  }

  listPlans(): Observable<ApiResponse<TenantBillingPlan[]>> {
    return this.http.get<ApiResponse<TenantBillingPlan[]>>(`${this.API_URL}/available-plans`);
  }

  createCheckoutSession(
    request: TenantCheckoutSessionRequest
  ): Observable<ApiResponse<TenantCheckoutSessionResponse>> {
    return this.http.post<ApiResponse<TenantCheckoutSessionResponse>>(
      `${this.API_URL}/checkout`,
      request
    );
  }

  getCheckoutResult(
    stripeSessionId: string
  ): Observable<ApiResponse<TenantCheckoutResultResponse>> {
    return this.http.get<ApiResponse<TenantCheckoutResultResponse>>(
      `${this.API_URL}/checkout-result`,
      { params: { session_id: stripeSessionId } }
    );
  }
}
