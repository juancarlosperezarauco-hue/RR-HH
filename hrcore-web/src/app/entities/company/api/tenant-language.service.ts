import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { CompanyLanguage } from '../model/company-language-translations';

export interface TenantLanguageResponse {
  language: CompanyLanguage;
}

@Injectable({ providedIn: 'root' })
export class TenantLanguageService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/api/settings/tenant/language`;

  get() {
    return this.http.get<ApiResponse<TenantLanguageResponse>>(this.url).pipe(map(checked));
  }

  supported() {
    return this.http.get<ApiResponse<string[]>>(`${this.url}/supported`).pipe(map(response => {
      if (!response.success || !Array.isArray(response.data)) {
        throw new Error(response.message || 'No se pudieron consultar los idiomas disponibles.');
      }
      return response.data;
    }));
  }

  save(language: CompanyLanguage) {
    return this.http.put<ApiResponse<TenantLanguageResponse>>(this.url, { language }).pipe(map(checked));
  }
}

function checked(response: ApiResponse<TenantLanguageResponse>): ApiResponse<TenantLanguageResponse> {
  if (!response.success) throw new Error(response.message || 'La operación no se completó.');
  return response;
}
