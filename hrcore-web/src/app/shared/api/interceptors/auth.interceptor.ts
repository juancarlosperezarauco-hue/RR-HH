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
}
