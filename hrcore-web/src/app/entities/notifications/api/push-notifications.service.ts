import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { initializeApp, getApps } from 'firebase/app';
import { getMessaging, getToken, isSupported, onMessage, Messaging } from 'firebase/messaging';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';

type DeviceResponse = { id: string; fcmToken: string; platform: string };

@Injectable({ providedIn: 'root' })
export class PushNotificationsService {
  private readonly http = inject(HttpClient);
  private messaging: Messaging | null = null;
  private initialized = false;

  async initialize(): Promise<void> {
    if (this.initialized || typeof window === 'undefined' || !('Notification' in window)) return;
    const config = environment.firebaseWeb;
    if (!config.apiKey || !config.projectId || !config.messagingSenderId || !config.appId || !config.vapidKey) return;
    if (!(await isSupported())) return;
    if (Notification.permission === 'denied') return;
    const permission = Notification.permission === 'granted' ? permissionValue() : await Notification.requestPermission();
    if (permission !== 'granted') return;
    const app = getApps().length ? getApps()[0] : initializeApp({
      apiKey: config.apiKey, authDomain: config.authDomain, projectId: config.projectId,
      storageBucket: config.storageBucket, messagingSenderId: config.messagingSenderId,
      appId: config.appId, measurementId: config.measurementId || undefined
    });
    this.messaging = getMessaging(app);
    const encodedConfig = btoa(unescape(encodeURIComponent(JSON.stringify({
      apiKey: config.apiKey, authDomain: config.authDomain, projectId: config.projectId,
      storageBucket: config.storageBucket, messagingSenderId: config.messagingSenderId,
      appId: config.appId, measurementId: config.measurementId || undefined
    }))));
    const registration = await navigator.serviceWorker.register(`/firebase-messaging-sw.js?config=${encodeURIComponent(encodedConfig)}`);
    const token = await getToken(this.messaging, { vapidKey: config.vapidKey, serviceWorkerRegistration: registration });
    if (!token) return;
    await firstValueFrom(this.http.post<ApiResponse<DeviceResponse>>('/api/me/notification-devices', {
      deviceId: this.deviceId(), fcmToken: token, platform: 'WEB',
      deviceName: navigator.userAgent.slice(0, 120), appVersion: 'web', osVersion: navigator.platform
    }));
    onMessage(this.messaging, () => window.dispatchEvent(new CustomEvent('hrcore:push-notification')));
    this.initialized = true;
  }

  async registerCurrentDevice(): Promise<void> {
    this.initialized = false;
    await this.initialize();
  }

  private deviceId(): string {
    const key = 'hrcore.notification.device-id';
    const current = localStorage.getItem(key);
    if (current) return current;
    const created = crypto.randomUUID(); localStorage.setItem(key, created); return created;
  }
}

function permissionValue(): NotificationPermission { return 'granted'; }
