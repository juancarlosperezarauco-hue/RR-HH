import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';

export type TenantDateFormat = 'DMY' | 'MDY' | 'YMD';
export type TenantNumberFormat = 'DECIMAL_COMMA' | 'DECIMAL_POINT';

export interface TenantRegionalFormats {
  dateFormat: TenantDateFormat;
  numberFormat: TenantNumberFormat;
}

@Injectable({ providedIn: 'root' })
export class TenantRegionalFormatsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/api/settings/tenant/regional-formats`;

  get() {
    return this.http.get<ApiResponse<TenantRegionalFormats>>(this.url).pipe(map(checked));
  }

  save(formats: TenantRegionalFormats) {
    return this.http.put<ApiResponse<TenantRegionalFormats>>(this.url, formats).pipe(map(checked));
  }
}

function checked(response: ApiResponse<TenantRegionalFormats>): ApiResponse<TenantRegionalFormats> {
  if (!response.success) throw new Error(response.message || 'La operación no se completó.');
  return response;
}
