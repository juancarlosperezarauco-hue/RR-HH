import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';
import { NotificationResponse, PageResponse, UnreadNotificationCountResponse } from '../model/notifications.model';

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
