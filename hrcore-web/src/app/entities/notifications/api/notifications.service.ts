import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { NotificationDeviceResponse, NotificationPreferenceResponse, NotificationResponse, PageResponse, UnreadNotificationCountResponse } from '../model/notifications.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/api/me`;

  listNotifications(page = 0, size = 20): Observable<ApiResponse<PageResponse<NotificationResponse>>> {
    return this.http.get<ApiResponse<PageResponse<NotificationResponse>>>(`${this.API_URL}/notifications`, {
      params: { page, size }
    });
  }

  unreadCount(): Observable<ApiResponse<UnreadNotificationCountResponse>> {
    return this.http.get<ApiResponse<UnreadNotificationCountResponse>>(`${this.API_URL}/notifications/unread-count`);
  }

  listDevices(page = 0, size = 50): Observable<ApiResponse<PageResponse<NotificationDeviceResponse>>> {
    return this.http.get<ApiResponse<PageResponse<NotificationDeviceResponse>>>(`${this.API_URL}/notification-devices`, {
      params: { page, size }
    });
  }

  deactivateDevice(id: string): Observable<ApiResponse<NotificationDeviceResponse>> {
    return this.http.patch<ApiResponse<NotificationDeviceResponse>>(`${this.API_URL}/notification-devices/${id}/deactivate`, {});
  }

  listPreferences(page = 0, size = 50): Observable<ApiResponse<PageResponse<NotificationPreferenceResponse>>> {
    return this.http.get<ApiResponse<PageResponse<NotificationPreferenceResponse>>>(`${this.API_URL}/notification-preferences`, {
      params: { page, size }
    });
  }

  getNotificationById(id: string): Observable<ApiResponse<NotificationResponse>> {
    return this.http.get<ApiResponse<NotificationResponse>>(`${this.API_URL}/notifications/${id}`);
  }

  markNotificationAsRead(id: string): Observable<ApiResponse<NotificationResponse>> {
    return this.http.patch<ApiResponse<NotificationResponse>>(`${this.API_URL}/notifications/${id}/read`, {});
  }

  openNotification(id: string): Observable<ApiResponse<NotificationResponse>> {
    return this.http.patch<ApiResponse<NotificationResponse>>(`${this.API_URL}/notifications/${id}/open`, {});
  }

  archiveNotification(id: string): Observable<ApiResponse<NotificationResponse>> {
    return this.http.patch<ApiResponse<NotificationResponse>>(`${this.API_URL}/notifications/${id}/archive`, {});
  }

  markAllAsRead(): Observable<ApiResponse<unknown>> {
    return this.http.patch<ApiResponse<unknown>>(`${this.API_URL}/notifications/read-all`, {});
  }
}
