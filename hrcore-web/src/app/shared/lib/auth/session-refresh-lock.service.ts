import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SessionRefreshLockService {
  async run<T>(scope: 'TENANT' | 'PLATFORM', work: () => Promise<T>): Promise<T> {
    const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined;
    if (!locks) throw new Error('Web Locks API is required to refresh the session');
    return locks.request(`hrcore-auth-refresh:${scope}`, { mode: 'exclusive' }, work);
  }
}
