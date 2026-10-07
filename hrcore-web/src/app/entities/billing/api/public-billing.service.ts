import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { PublicBillingPlan } from '../model/public-billing-plan.model';
import { PublicCheckoutActivationStatusResponse } from '../model/public-checkout-activation-status.model';

@Injectable({
  providedIn: 'root'
})
export class PublicBillingService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/api/public`;

  listPublicPlans(): Observable<ApiResponse<PublicBillingPlan[]>> {
    return this.http.get<ApiResponse<PublicBillingPlan[]>>(`${this.API_URL}/plans`);
  }

  getCheckoutActivationStatus(
    stripeSessionId: string
  ): Observable<ApiResponse<PublicCheckoutActivationStatusResponse>> {
    return this.http.get<ApiResponse<PublicCheckoutActivationStatusResponse>>(
      `${this.API_URL}/checkout-sessions/${encodeURIComponent(stripeSessionId)}/status`
    );
  }
}
