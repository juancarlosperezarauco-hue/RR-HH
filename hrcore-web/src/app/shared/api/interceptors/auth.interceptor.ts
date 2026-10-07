import { HttpBackend, HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, of, switchMap, throwError } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { AuthTokenResponse } from '../../../entities/auth';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';
import { PlatformStorageService } from '../../../features/platform/lib/platform-storage.service';
import { PlatformAuthTokenResponse } from '../../../entities/platform/api/platform.service';
import { environment } from '../../../../environments/environment';

const PLATFORM_REFRESH_HEADER = 'X-Platform-Refresh-Retry';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  if (isPublicApiRequest(req.url)) {
    return next(req);
  }

  const authStorage = inject(AuthStorageService);
  const platformStorage = inject(PlatformStorageService);
  const router = inject(Router);
  const httpBackend = inject(HttpBackend);
  const refreshHttp = new HttpClient(httpBackend);

  const clonedRequest = addAuthorizationHeaders(req, authStorage, platformStorage);

  return next(clonedRequest).pipe(
    catchError((error: unknown) => {
      if (!shouldAttemptPlatformRefresh(req, error, platformStorage)) {
        if (!shouldAttemptTenantRefresh(req, error, authStorage)) {
          return throwError(() => error);
        }

        return refreshTenantSession(refreshHttp, authStorage).pipe(
          switchMap((accessToken) => {
            const retryRequest = req.clone({
              setHeaders: {
                Authorization: `Bearer ${accessToken}`,
                'X-Tenant-Slug': authStorage.getTenantSlug() ?? ''
              }
            });
            return next(retryRequest);
          }),
          catchError((refreshError) => {
            authStorage.clear();
            void router.navigate(['/login'], { replaceUrl: true });
            return throwError(() => refreshError);
          })
        );
      }

      return refreshPlatformSession(refreshHttp, platformStorage).pipe(
        switchMap((accessToken) => {
          const retryRequest = req.clone({
            setHeaders: {
              Authorization: `Bearer ${accessToken}`,
              [PLATFORM_REFRESH_HEADER]: '1'
            }
          });
          return next(retryRequest);
        }),
        catchError((refreshError) => {
          platformStorage.clearSession();
          void router.navigate(['/platform/login'], { replaceUrl: true });
          return throwError(() => refreshError);
        })
      );
    })
  );
};

function addAuthorizationHeaders(
  req: Parameters<HttpInterceptorFn>[0],
  authStorage: AuthStorageService,
  platformStorage: PlatformStorageService
) {
  if (isPublicApiRequest(req.url)) {
    return req;
  }

  if (
    req.url.includes('/api/platform/auth/login') ||
    req.url.includes('/api/platform/auth/refresh') ||
    req.url.includes('/api/auth/login') ||
    req.url.includes('/api/auth/refresh') ||
    req.url.includes('/api/auth/face/login') ||
    req.url.includes('/api/auth/forgot-password') ||
    req.url.includes('/api/auth/reset-password')
  ) {
    return req;
  }

  if (req.url.includes('/api/platform/')) {
    const platformToken = platformStorage.getAccessToken();
    if (!platformToken) {
      return req;
    }

    return req.clone({
      setHeaders: {
        Authorization: `Bearer ${platformToken}`
      }
    });
  }

  if (req.url.includes('/api/')) {
    const token = authStorage.getToken();
    const tenantSlug = authStorage.getTenantSlug();
    const headersConfig: Record<string, string> = {};

    if (token && !req.headers.has('Authorization')) {
      headersConfig['Authorization'] = `Bearer ${token}`;
    }

    if (tenantSlug && !req.headers.has('X-Tenant-Slug')) {
      headersConfig['X-Tenant-Slug'] = tenantSlug;
    }

    if (Object.keys(headersConfig).length > 0) {
      return req.clone({ setHeaders: headersConfig });
    }
  }

  return req;
}

function isPublicApiRequest(url: string): boolean {
  return url.includes('/api/public/');
}

function shouldAttemptPlatformRefresh(
  req: Parameters<HttpInterceptorFn>[0],
  error: unknown,
  platformStorage: PlatformStorageService
): boolean {
  if (!(error instanceof HttpErrorResponse)) {
    return false;
  }

  if (error.status !== 401) {
    return false;
  }

  if (isPublicApiRequest(req.url)) {
    return false;
  }

  if (!req.url.includes('/api/platform/')) {
    return false;
  }

  if (req.url.includes('/api/platform/auth/login') || req.url.includes('/api/platform/auth/refresh')) {
    return false;
  }

  if (req.headers.has(PLATFORM_REFRESH_HEADER)) {
    return false;
  }

  return platformStorage.hasRefreshToken();
}

function shouldAttemptTenantRefresh(
  req: Parameters<HttpInterceptorFn>[0],
  error: unknown,
  authStorage: AuthStorageService
): boolean {
  if (!(error instanceof HttpErrorResponse)) {
    return false;
  }

  if (error.status !== 401) {
    return false;
  }

  if (isPublicApiRequest(req.url)) {
    return false;
  }

  if (req.url.includes('/api/platform/')) {
    return false;
  }

  if (!req.url.includes('/api/')) {
    return false;
  }

  if (
    req.url.includes('/api/auth/login') ||
    req.url.includes('/api/auth/refresh') ||
    req.url.includes('/api/auth/face/login') ||
    req.url.includes('/api/auth/forgot-password') ||
    req.url.includes('/api/auth/reset-password')
  ) {
    return false;
  }

  if (req.headers.has(PLATFORM_REFRESH_HEADER)) {
    return false;
  }

  return authStorage.hasRefreshToken() && authStorage.hasTenantSlug();
}

function refreshPlatformSession(
  refreshHttp: HttpClient,
  platformStorage: PlatformStorageService
) {
  const refreshToken = platformStorage.getRefreshToken();
  if (!refreshToken) {
    return throwError(() => new Error('Missing platform refresh token'));
  }

  return refreshHttp
    .post<ApiResponse<PlatformAuthTokenResponse>>(`${environment.apiUrl}/api/platform/auth/refresh`, { refreshToken })
    .pipe(
      switchMap((response) => {
        if (!response.success || !response.data?.accessToken) {
          return throwError(() => new Error(response.message || 'Platform token refresh failed'));
        }

        platformStorage.saveAccessToken(response.data.accessToken);
        if (response.data.refreshToken) {
          platformStorage.saveRefreshToken(response.data.refreshToken);
        }

        return of(response.data.accessToken);
      })
    );
}

function refreshTenantSession(
  refreshHttp: HttpClient,
  authStorage: AuthStorageService
) {
  const refreshToken = authStorage.getRefreshToken();
  const tenantSlug = authStorage.getTenantSlug();

  if (!refreshToken) {
    return throwError(() => new Error('Missing tenant refresh token'));
  }

  if (!tenantSlug) {
    return throwError(() => new Error('Missing tenant slug'));
  }

  return refreshHttp
    .post<ApiResponse<AuthTokenResponse>>(
      `${environment.apiUrl}/api/auth/refresh`,
      { refreshToken },
      { headers: { 'X-Tenant-Slug': tenantSlug } }
    )
    .pipe(
      switchMap((response) => {
        if (!response.success || !response.data?.accessToken) {
          return throwError(() => new Error(response.message || 'Tenant token refresh failed'));
        }

        authStorage.saveToken(response.data.accessToken);
        if (response.data.refreshToken) {
          authStorage.saveRefreshToken(response.data.refreshToken);
        }

        return of(response.data.accessToken);
      })
    );
}
