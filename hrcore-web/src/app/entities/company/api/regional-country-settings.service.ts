import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';

export interface RegionalHoliday { date: string; name: string }
export interface RegionalHolidays { holidays: RegionalHoliday[] }
export interface RegionalWorkday {
  days: string[];
  start: string | null;
  end: string | null;
  breakStart: string | null;
  breakEnd: string | null;
}
export interface RegionalWorkdayView extends RegionalWorkday {
  dailyMinutes: number;
  weeklyMinutes: number;
}
export interface RegionalRules {
  contractReminderDays: number | null;
  shiftBufferMinutes: number | null;
  attendanceToleranceMinutes: number | null;
  absenceNoticeHours: number | null;
}

@Injectable({ providedIn: 'root' })
export class RegionalCountrySettingsService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/settings/tenant/regional/countries`;
  private url(country: string, section: string): string {
    if (!/^[A-Z]{2}$/.test(country)) throw new Error('Código de país inválido');
    return `${this.base}/${country}/${section}`;
  }
  holidays(country: string) {
    return this.http.get<ApiResponse<RegionalHolidays>>(this.url(country, 'holidays')).pipe(map(checked));
  }
  saveHolidays(country: string, value: RegionalHolidays) {
    return this.http.put<ApiResponse<RegionalHolidays>>(this.url(country, 'holidays'), value).pipe(map(checked));
  }
  workday(country: string) {
    return this.http.get<ApiResponse<RegionalWorkdayView>>(this.url(country, 'workday')).pipe(map(checked));
  }
  saveWorkday(country: string, value: RegionalWorkday) {
    return this.http.put<ApiResponse<RegionalWorkdayView>>(this.url(country, 'workday'), value).pipe(map(checked));
  }
  rules(country: string) {
    return this.http.get<ApiResponse<RegionalRules>>(this.url(country, 'rules')).pipe(map(checked));
  }
  saveRules(country: string, value: RegionalRules) {
    return this.http.put<ApiResponse<RegionalRules>>(this.url(country, 'rules'), value).pipe(map(checked));
  }
}

function checked<T>(response: ApiResponse<T>): T {
  if (!response.success || response.data == null) throw new Error(response.message || 'La operación no se completó');
  return response.data;
}
