import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/**
 * Keeps the backend contract intact while making the actionable details
 * available to existing consumers that read `error.message`.
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => next(request).pipe(
  catchError((error: unknown) => {
    if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object' && Array.isArray(error.error.errors)) {
      const details = error.error.errors
        .filter((detail: unknown): detail is string => typeof detail === 'string' && detail.trim().length > 0)
        .map((detail: string) => detail.trim());
      if (details.length > 0) {
        return throwError(() => new HttpErrorResponse({
          error: { ...error.error, message: details.join(' ') },
          headers: error.headers,
          status: error.status,
          statusText: error.statusText,
          url: error.url ?? undefined
        }));
      }
    }
    return throwError(() => error);
  })
);
