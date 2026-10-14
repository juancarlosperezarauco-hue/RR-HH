import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthStorageService } from '../../lib/storage/auth-storage.service';
import { PlatformStorageService } from '../../../features/platform/lib/platform-storage.service';
import { SessionCoordinatorService } from '../../lib/auth/session-coordinator.service';

export const AUTH_RETRIED = new HttpContextToken<boolean>(() => false);

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const target = classify(req.url);
  if (!target) return next(req);
  const tenantStorage = inject(AuthStorageService);
  const platformStorage = inject(PlatformStorageService);
<<<<<<< HEAD
  const sessions = inject(SessionCoordinatorService);
  const session = target.scope === 'TENANT' ? tenantStorage.getSession() : platformStorage.getSession();
  const isAuthRoute = target.path.includes('/auth/login') || target.path.includes('/auth/refresh') || target.path.includes('/auth/logout') || target.path.includes('/forgot-password') || target.path.includes('/reset-password') || target.path.includes('/activate-account') || target.path.includes('/resend-activation') || target.path.includes('/face/login');
  let request = req;
  if (session && (!isAuthRoute || target.path.endsWith('/logout'))) {
    const headers: Record<string, string> = { Authorization: `Bearer ${session.accessToken}` };
    if (target.scope === 'TENANT' && session.tenantSlug) headers['X-Tenant-Slug'] = session.tenantSlug;
    request = req.clone({ setHeaders: headers });
  }
  return next(request).pipe(catchError((error: unknown) => {
    if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isAuthRoute || req.context.get(AUTH_RETRIED) || !session || sessions.isLoggingOut(target.scope)) return throwError(() => error);
    return from(sessions.refreshSingleFlight(target.scope, session)).pipe(
      switchMap(nextSession => {
        const latest = sessions.getSession(target.scope);
        if (!latest || latest.loginId !== session.loginId || latest.tenantSlug !== session.tenantSlug || nextSession.loginId !== session.loginId) return throwError(() => new Error('Session changed'));
        const headers: Record<string, string> = { Authorization: `Bearer ${nextSession.accessToken}` };
        if (target.scope === 'TENANT' && nextSession.tenantSlug) headers['X-Tenant-Slug'] = nextSession.tenantSlug;
        return next(req.clone({ context: req.context.set(AUTH_RETRIED, true), setHeaders: headers }));
      }),
      catchError(refreshError => { sessions.clearSession(target.scope, session.loginId); return throwError(() => refreshError); })
    );
  }));
};

function classify(rawUrl: string): { scope: 'TENANT' | 'PLATFORM'; path: string } | null {
  const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost';
  let url: URL;
  try { url = new URL(rawUrl, environment.apiUrl || origin); } catch { return null; }
  const base = new URL(environment.apiUrl || origin, origin);
  if (url.origin !== base.origin || !url.pathname.startsWith(`${base.pathname.replace(/\/$/, '')}/api/`)) return null;
  if (url.pathname.startsWith(`${base.pathname.replace(/\/$/, '')}/api/public/`)) return null;
  const path = url.pathname;
  return { path, scope: path.startsWith(`${base.pathname.replace(/\/$/, '')}/api/platform/`) ? 'PLATFORM' : 'TENANT' };
=======
  const router = inject(Router);
  const httpBackend = inject(HttpBackend);
  const refreshHttp = new HttpClient(httpBackend);

  const clonedRequest = addAuthorizationHeaders(req, authStorage, platformStorage);

  return next(clonedRequest).pipe(
    catchError((error: unknown) => {
      // Si estamos en modo mock, no intentar refresco contra el backend ni forzar logout
      if (environment.useMocks) {
        return throwError(() => error);
      }

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
  if (environment.useMocks) {
    return false;
  }

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
>>>>>>> main
}
