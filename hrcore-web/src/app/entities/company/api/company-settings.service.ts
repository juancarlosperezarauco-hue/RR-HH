import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { CompanySettings, CompanySetupStatus, HrSettings } from '../model/company.model';

@Injectable({ providedIn: 'root' })
export class CompanySettingsService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/settings/tenant`;

  getSettings() { return this.http.get<ApiResponse<CompanySettings>>(this.api).pipe(map(checked)); }
  updateSettings(value: CompanySettings) { return this.http.put<ApiResponse<CompanySettings>>(this.api, value).pipe(map(checked)); }
  getHrSettings() { return this.http.get<ApiResponse<HrSettings>>(`${this.api}/hr`).pipe(map(checked)); }
  updateHrSettings(value: HrSettings) { return this.http.put<ApiResponse<HrSettings>>(`${this.api}/hr`, value).pipe(map(checked)); }
  getSetupStatus() { return this.http.get<ApiResponse<CompanySetupStatus>>(`${this.api}/setup-status`).pipe(map(checked)); }
  completeSetup(confirmedTimezone: string) { return this.http.post<ApiResponse<CompanySetupStatus>>(`${this.api}/complete-setup`, { confirmedTimezone }).pipe(map(checked)); }
}

function checked<T>(response: ApiResponse<T>): ApiResponse<T> {
  if (!response.success) throw new Error(response.message || 'La operación no se completó.');
  return response;
}
