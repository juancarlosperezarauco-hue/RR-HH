import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { AppearanceColors, ColorMode } from '../../../pages/appearance-settings-page/appearance-palette';

export interface AppearanceSettings extends AppearanceColors {
  preset: string;
  mode: ColorMode;
}

@Injectable({ providedIn: 'root' })
export class AppearanceSettingsService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/api/settings/tenant/appearance`;

  get() {
    return this.http.get<ApiResponse<AppearanceSettings>>(this.api).pipe(map(checked));
  }

  save(value: AppearanceSettings) {
    return this.http.put<ApiResponse<AppearanceSettings>>(this.api, value).pipe(map(checked));
  }
}

function checked(response: ApiResponse<AppearanceSettings>): ApiResponse<AppearanceSettings> {
  if (!response?.success || !response.data) {
    throw new Error(response?.message || 'La operación de apariencia no se completó.');
  }
  return response;
}
