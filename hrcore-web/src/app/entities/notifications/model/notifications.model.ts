export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort?: unknown;
    offset?: number;
    unpaged?: boolean;
    paged?: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort?: unknown;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export interface NotificationResponse {
  id: string;
  userId: string;
  type: string;
  category: string;
  priority: string;
  status: string;
  title: string;
  body: string;
  data: Record<string, unknown> | null;
  imageUrl: string | null;
  actionUrl: string | null;
  readAt: string | null;
  openedAt: string | null;
  archivedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UnreadNotificationCountResponse {
  unreadCount: number;
}

export interface NotificationDeviceResponse {
  id: string;
  userId: string;
  deviceId: string;
  platform: string;
  deviceName: string | null;
  appVersion: string | null;
  osVersion: string | null;
  status: string;
  lastSeenAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface NotificationPreferenceResponse {
  id: string;
  userId: string;
  category: string;
  pushEnabled: boolean;
  inAppEnabled: boolean;
  emailEnabled: boolean;
  smsEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}
