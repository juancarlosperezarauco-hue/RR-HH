import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SessionRefreshLockService {
  private readonly fallbackLocks = new Map<'TENANT' | 'PLATFORM', Promise<unknown>>();

  async run<T>(scope: 'TENANT' | 'PLATFORM', work: () => Promise<T>): Promise<T> {
    const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
    if (locks) {
      return locks.request(`hrcore-auth-refresh:${scope}`, { mode: 'exclusive' }, work);
    }

    // Algunos navegadores o contextos embebidos no exponen Web Locks. En ese
    // caso serializamos las renovaciones dentro de esta pestaña para no perder
    // la sesión cuando el access token expire durante una recarga.
    const previous = this.fallbackLocks.get(scope) ?? Promise.resolve();
    const current = previous.catch(() => undefined).then(work);
    this.fallbackLocks.set(scope, current);
    try {
      return await current;
    } finally {
      if (this.fallbackLocks.get(scope) === current) {
        this.fallbackLocks.delete(scope);
      }
    }
  }
}
